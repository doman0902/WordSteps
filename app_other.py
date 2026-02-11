import fastapi
import uvicorn
from pydantic import BaseModel
from typing import List
import random
import os
import pickle
import numpy as np

# --- 1. ML ÉS NYELVI IMPORT ---
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import tensorflow as tf

from sqlalchemy import create_engine, Column, Integer, String, func, ForeignKey
from sqlalchemy.orm import sessionmaker, Session, relationship
from fastapi import Depends
from sqlalchemy.orm import DeclarativeBase 
from contextlib import asynccontextmanager
import pandas as pd

# --- Adatbázis konfiguráció ---
SQLALCHEMY_DATABASE_URL = "sqlite:///./words.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

class Base(DeclarativeBase): pass

# 1. TÁBLA: Alapszavak
class DbWord(Base):
    __tablename__ = "words"
    id = Column(Integer, primary_key=True, index=True)
    correct = Column(String, unique=True, index=True)
    cefr_level = Column(String) 
    
    quiz_data = relationship("DbQuizData", back_populates="word_ref")

# 2. TÁBLA: Előgenerált Kvíz Feladatok
class DbQuizData(Base):
    __tablename__ = "quiz_data"
    id = Column(Integer, primary_key=True, index=True)
    word_id = Column(Integer, ForeignKey("words.id"))
    distractor_1 = Column(String)
    distractor_2 = Column(String)
    distractor_3 = Column(String)
    
    word_ref = relationship("DbWord", back_populates="quiz_data")

class QuizWordResponse(BaseModel):
    id: int
    cefr_level: str
    correct: str
    options: List[str]

# --- 2. LIFESPAN (BETÖLTÉS ÉS DINAMIKUS SEEDING) ---
@asynccontextmanager
async def lifespan(app: fastapi.FastAPI):
    print("Szerver indul, adatbázis szinkronizálása...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        # Ellenőrizzük az alapszavakat
        if db.query(DbWord).count() == 0:
            print("Adatbázis üres, Master lista betöltése...")
            master_file = 'b2_words_master.csv'
            
            if os.path.exists(master_file):
                df = pd.read_csv(master_file)
                new_words = []
                for _, row in df.iterrows():
                    word_str = str(row['word']).lower().strip()
                    level_str = str(row['level']).upper().strip()
                    
                    if len(word_str) > 2:
                        new_words.append(DbWord(correct=word_str, cefr_level=level_str))
                
                db.add_all(new_words)
                db.commit()
                print(f"Sikeres seeding: {len(new_words)} szó beimportálva.")
            else:
                print(f"HIBA: A '{master_file}' nem található!")

        quiz_count = db.query(DbQuizData).count()
        print(f"Kész feladatok száma: {quiz_count}")
            
    finally:
        db.close()
    yield

app = fastapi.FastAPI(lifespan=lifespan)

# --- 3. API VÉGPONTOK ---

# ÚJ: Státusz ellenőrző végpont, hogy lásd hol tart a generálás
@app.get("/status")
def get_status(db: Session = Depends(lambda: SessionLocal())):
    total_words = db.query(DbWord).count()
    ready_quizzes = db.query(DbQuizData).count()
    percentage = (ready_quizzes / total_words * 100) if total_words > 0 else 0
    return {
        "total_words_in_db": total_words,
        "ready_quizzes": ready_quizzes,
        "progress_percentage": f"{percentage:.2f}%",
        "message": "Futtasd a pre-generator.py-t a szám növeléséhez!"
    }

@app.get("/quiz_word", response_model=QuizWordResponse)
def get_quiz_word(db: Session = Depends(lambda: SessionLocal())):
    # Csak olyan szót adunk ki, amihez van már MI-vel gyártott feladat
    quiz_entry = db.query(DbQuizData).order_by(func.random()).first()
    
    if not quiz_entry:
        raise fastapi.HTTPException(
            status_code=503, 
            detail="Nincsenek kész feladatok. Futtasd le a pre-generator.py-t!"
        )

    correct = quiz_entry.word_ref.correct
    options = [
        correct,
        quiz_entry.distractor_1,
        quiz_entry.distractor_2,
        quiz_entry.distractor_3
    ]
    random.shuffle(options)

    return QuizWordResponse(
        id=quiz_entry.id,
        cefr_level=quiz_entry.word_ref.cefr_level,
        correct=correct,
        options=options
    )

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)