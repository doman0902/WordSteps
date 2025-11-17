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

def swap_vowel(word):
    """Felcserél egy véletlenszerű magánhangzót egy másikra."""
    vowels = 'aeiou'
    vowel_indices = [i for i, char in enumerate(word) if char in vowels]
    if not vowel_indices:
        return word

    idx_to_swap = random.choice(vowel_indices)
    original_vowel = word[idx_to_swap]
    new_vowel = random.choice([v for v in vowels if v != original_vowel])
    return word[:idx_to_swap] + new_vowel + word[idx_to_swap+1:]

def omit_double_letter(word):
    """Elhagy egyet egy dupla mássalhangzóból (pl. address -> adres)."""
    for i in range(len(word) - 1):
        if word[i] == word[i+1] and word[i] not in 'aeiou':
            return word[:i] + word[i+1:]
    return word

def transpose_letters(word):
    """Felcserél két szomszédos betűt (pl. friend -> freind)."""
    if len(word) < 2:
        return word
    idx = random.randint(0, len(word) - 2)
    return word[:idx] + word[idx+1] + word[idx] + word[idx+2:]

def swap_phonetic(word):
    """Fonetikus cserét végez (pl. ph -> f)."""
    if 'ph' in word:
        return word.replace('ph', 'f', 1)
    return word

transformation_strategies = [swap_vowel, omit_double_letter, transpose_letters, swap_phonetic]

def generate_smart_distractors(correct_word: str) -> List[str]:
    """
    Generál 2 db egyedi, "okos" disztraktort a megadott stratégiák alapján.
    """
    distractors = set()
    
    attempts = 0
    while len(distractors) < 2 and attempts < 10:
        strategy = random.choice(transformation_strategies)
        new_word = strategy(correct_word)
        
        if new_word != correct_word and new_word not in distractors:
            distractors.add(new_word)
        attempts += 1
        
    while len(distractors) < 2:
        distractors.add(correct_word + random.choice(['a', 'x', 'z']))

    return list(distractors)

@asynccontextmanager
async def lifespan(app: fastapi.FastAPI):
    print("Szerver indul, adatbázis ellenőrzése...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        count = db.query(DbWord).count()
        if count == 0:
            print("Adatbázis üres, feltöltés (seeding) indul...")
            try:
                df = pd.read_csv('b2_words.csv',sep=';',encoding='latin1')
                print(f"Beolvasva {len(df)} szó a CSV fájlból.")

                words_from_csv = df['word'].tolist()
                new_words_to_db = []

                for word in words_from_csv:
                    new_words_to_db.append(DbWord(correct=str(word).lower(), cefr_level="B2"))

                db.add_all(new_words_to_db)
                db.commit()
                print(f"Adatbázis feltöltve {len(new_words_to_db)} szóval.")

            except FileNotFoundError:
                print(f"HIBA: A '{'b2_words.csv'}' fájl nem található! Az adatbázis üres marad.")
                print("Kérlek, töltsd le a CSV-t és mentsd az app.py mellé.")
            except KeyError:
                print(f"HIBA: Nem található 'word' oszlop a '{'b2_words.csv'}'-ban.")
            except Exception as e:
                print(f"HIBA a CSV beolvasása vagy adatbázisba írása közben: {e}")
            
        else:
            print(f"Adatbázis már tartalmazza  a {count} szót.")
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
    random_word_from_db = db.query(DbWord).order_by(func.random()).first()

    if not random_word_from_db:
        raise fastapi.HTTPException(status_code=404, detail="Nincsenek szavak az adatbázisban")

    correct=random_word_from_db.correct
    distractors=generate_smart_distractors(correct)
    options=[correct]+distractors
    random.shuffle(options)

    response_data = QuizWordResponse(
        id=random_word_from_db.id,
        correct=random_word_from_db.correct,
        cefr_level=random_word_from_db.cefr_level,
        options=options
    )
    print("Kvíz szó küldve:", response_data.correct)
    return response_data

if __name__ == "__main__":
    print("Teszt indul a http://localhost:8000/test címen...")
    print("Adatbázis fájl: words.db")
    print("Kvíz szó elérhető a http://localhost:8000/quiz_word címen...")
    uvicorn.run(app, host="0.0.0.0", port=8000)