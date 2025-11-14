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

@asynccontextmanager
async def lifespan(app: fastapi.FastAPI):
    print("Szerver indul, adatbázis ellenőrzése...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        count = db.query(DbWord).count()
        if count == 0:
            print("Adatbázis üres, feltöltés (seeding) indul...")
            example_words = [
                DbWord(correct="apple", cefr_level="A1"),
                DbWord(correct="house", cefr_level="A1"),
                DbWord(correct="beautiful", cefr_level="B1")
            ]
            db.add_all(example_words)
            db.commit()
            print("Adatbázis feltöltve 3 szóval.")
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
    distractors=[correct[1:] + "a", correct + "xyz"]
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