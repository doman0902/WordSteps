import fastapi
import uvicorn
from pydantic import BaseModel
from typing import List
import random
from sqlalchemy import create_engine, Column, Integer, String, func
from sqlalchemy.orm import sessionmaker, Session
from fastapi import Depends
from sqlalchemy.orm import DeclarativeBase 
from contextlib import asynccontextmanager
import pandas as pd
import os
import numpy as np
# --- 1. ML IMPORT ÉS KONFIGURÁCIÓ ---
# Kikapcsoljuk a felesleges TensorFlow naplózást
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import tensorflow as tf


SQLALCHEMY_DATABASE_URL = "sqlite:///./words.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
class Base(DeclarativeBase):
    pass

class DbWord(Base):
    __tablename__ = "words"
    id = Column(Integer, primary_key=True, index=True)
    correct = Column(String, unique=True, index=True)
    cefr_level = Column(String)


class QuizWordResponse(BaseModel):
    id:int
    cefr_level: str
    correct: str
    options: List[str]

# --- 2. ML GLOBÁLIS VÁLTOZÓK ---
ml_model = None
ml_metadata = None
encoder_model = None
decoder_model = None
valid_words_cache = set() # A létező szavak gyors ellenőrzéséhez (Collision check)

# --- 3. ML INFERENCIA (JÓSLÁS) LOGIKA ---
def setup_inference_models():
    """Rekonstruálja az encoder és decoder modelleket a betöltött h5 fájlból."""
    global ml_model, ml_metadata, encoder_model, decoder_model
    latent_dim = 256
    
    # Encoder modell leválasztása
    encoder_inputs = ml_model.input[0] 
    _, state_h_enc, state_c_enc = ml_model.layers[4].output 
    encoder_states = [state_h_enc, state_c_enc]
    encoder_model = tf.keras.Model(encoder_inputs, encoder_states)

    # Decoder modell leválasztása az önálló generáláshoz
    decoder_inputs = ml_model.input[1] 
    decoder_state_input_h = tf.keras.Input(shape=(latent_dim,))
    decoder_state_input_c = tf.keras.Input(shape=(latent_dim,))
    decoder_states_inputs = [decoder_state_input_h, decoder_state_input_c]
    
    decoder_lstm = ml_model.layers[5]
    decoder_embedding = ml_model.layers[3]
    
    decoder_outputs, state_h_dec, state_c_dec = decoder_lstm(
        decoder_embedding(decoder_inputs), initial_state=decoder_states_inputs
    )
    decoder_states = [state_h_dec, state_c_dec]
    decoder_dense = ml_model.layers[6]
    decoder_outputs = decoder_dense(decoder_outputs)
    
    decoder_model = tf.keras.Model(
        [decoder_inputs] + decoder_states_inputs,
        [decoder_outputs] + decoder_states
    )

def predict_misspelling(input_str):
    """Az MI modell segítségével generál egy elírást a bemeneti szóból."""
    if encoder_model is None or decoder_model is None:
        return None

    try:
        # Vektorizálás: a szót számokká alakítjuk
        input_seq = np.zeros((1, ml_metadata['max_encoder_seq_length']), dtype='float32')
        for t, char in enumerate(input_str.lower()):
            if char in ml_metadata['char_to_int']:
                input_seq[0, t] = ml_metadata['char_to_int'][char]

        # Állapotok kódolása az Encoderrel
        states_value = encoder_model.predict(input_seq, verbose=0)

        # Decoder indítása a start tokennel (\t)
        target_seq = np.zeros((1, 1))
        target_seq[0, 0] = ml_metadata['char_to_int']['\t']

        stop_condition = False
        decoded_sentence = ""
        
        while not stop_condition:
            output_tokens, h, c = decoder_model.predict([target_seq] + states_value, verbose=0)
            sampled_token_index = np.argmax(output_tokens[0, -1, :])
            sampled_char = ml_metadata['int_to_char'][sampled_token_index]
            
            if sampled_char == '\n' or len(decoded_sentence) > ml_metadata['max_decoder_seq_length']:
                stop_condition = True
            else:
                decoded_sentence += sampled_char

            target_seq = np.zeros((1, 1))
            target_seq[0, 0] = sampled_token_index
            states_value = [h, c]

        return decoded_sentence.strip()
    except Exception:
        return None


@asynccontextmanager
async def lifespan(app: fastapi.FastAPI):
    global ml_model, ml_metadata, valid_words_cache
    print("Szerver indul, adatbázis és MI ellenőrzése...")
    
    # MI Modell és Metaadatok betöltése
    try:
        if os.path.exists('spelling_model.h5') and os.path.exists('model_metadata.pkl'):
            ml_model = tf.keras.models.load_model('spelling_model.h5')
            with open('model_metadata.pkl', 'rb') as f:
                ml_metadata = pickle.load(f)
            setup_inference_models()
            print("MI modell sikeresen betöltve!")
        else:
            print("FIGYELEM: MI modell fájlok nem találhatók. Szabályalapú mód aktív.")
    except Exception as e:
        print(f"HIBA az MI modell betöltésekor: {e}")


    Base.metadata.create_all(bind=engine)  
    db = SessionLocal()
    try:
        if db.query(DbWord).count() == 0:
            print("Adatbázis üres, feltöltés (seeding) indul...")
            csv_file = 'b2_words_cleaned.csv'
            if os.path.exists(csv_file):
                # A korábbi hibák alapján: pontosvessző elválasztó és latin1 kódolás
                df = pd.read_csv(csv_file, sep=';', encoding='latin1')
                # Tisztítás és duplikátum szűrés
                clean_words = df['word'].str.lower().str.strip().drop_duplicates().tolist()
                db.add_all([DbWord(correct=w, cefr_level="B2") for w in clean_words if len(str(w)) > 2])
                db.commit()
                print(f"Adatbázis feltöltve {len(clean_words)} egyedi szóval.")
        
        # Validációs Cache feltöltése a Collision Checkhez
        all_words = db.query(DbWord.correct).all()
        valid_words_cache = {w[0] for w in all_words}
        print(f"Validációs szótár kész: {len(valid_words_cache)} szó.")

    finally:
        db.close()
    print("Szerver készen áll a fogadásra...")
    yield 
    
    print("Szerver leáll...")

app = fastapi.FastAPI(lifespan=lifespan)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/test")
async def test_endpoint():
    print("Kotlin hívás beérkezett")
    return {"message": "Python backend válaszolt"}

@app.get("/quiz_word", response_model=QuizWordResponse)
def get_quiz_word(db: Session=Depends(get_db)):
     # Véletlenszerű helyes szó lekérése
    while True:

        row = db.query(DbWord).order_by(func.random()).first()
        if not row:
            raise fastapi.HTTPException(status_code=404, detail="Nincsenek szavak az adatbázisban")

        correct = row.correct
        distractors = set()
            
        # Megpróbálunk 15-ször generálni (hogy meglegyen a 3 egyedi elírás)
        for _ in range(15):
            mi_dist = predict_misspelling(correct)
            # Feltételek: nem None, nem azonos a jóval, nem volt még, és NEM létező szó
            if mi_dist and mi_dist != correct and mi_dist not in distractors:
                if mi_dist not in valid_words_cache:
                    distractors.add(mi_dist)
            
            if len(distractors) >= 3:
                break
        
        # Ha sikerült 3-at generálni, megvagyunk!
        if len(distractors) >= 3:
            options = list(distractors) + [correct]
            random.shuffle(options)
            print(f"Sikeres generálás a '{correct}' szóra.")
            return QuizWordResponse(
                id=row.id,
                correct=correct,
                cefr_level=row.cefr_level,
                options=options
            )
        
        # Ha nem sikerült 3-at generálni, a ciklus újraindul egy másik véletlen szóval
        print(f"A '{correct}' szó elvetve (MI nem tudott 3 érvényes elírást). Új szó sorsolása...")

if __name__ == "__main__":
    print("Teszt indul a http://localhost:8000/test címen...")
    print("Adatbázis fájl: words.db")
    print("Kvíz szó elérhető a http://localhost:8000/quiz_word címen...")
    uvicorn.run(app, host="0.0.0.0", port=8000)