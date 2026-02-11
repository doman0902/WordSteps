import os
from sqlalchemy import create_engine, Column, Integer, String, ForeignKey
from sqlalchemy.orm import sessionmaker, relationship, DeclarativeBase

# --- ADATBÁZIS MODELL DEFINÍCIÓK (Meg kell egyezniük az app_other.py-val) ---
class Base(DeclarativeBase): pass

class DbWord(Base):
    __tablename__ = "words"
    id = Column(Integer, primary_key=True)
    correct = Column(String)
    cefr_level = Column(String)
    quiz_data = relationship("DbQuizData", back_populates="word_ref")

class DbQuizData(Base):
    __tablename__ = "quiz_data"
    id = Column(Integer, primary_key=True)
    word_id = Column(Integer, ForeignKey("words.id"))
    distractor_1 = Column(String)
    distractor_2 = Column(String)
    distractor_3 = Column(String)
    word_ref = relationship("DbWord", back_populates="quiz_data")

# --- LEKÉRDEZÉS ÉS KIÍRÁS ---
def check_database():
    db_path = "words.db"
    
    if not os.path.exists(db_path):
        print(f"Hiba: A '{db_path}' fájl nem található a mappában!")
        return

    # Kapcsolódás
    engine = create_engine(f"sqlite:///{db_path}")
    Session = sessionmaker(bind=engine)
    session = Session()

    # Lekérdezzük azokat a szavakat, amikhez már van generált kvíz adat
    # A JOIN biztosítja, hogy csak a kész feladatokat lássuk
    results = session.query(DbWord).join(DbQuizData).all()

    if not results:
        print("Az adatbázisban még nincsenek generált kvíz adatok.")
        print("Futtasd le a pre-generator.py-t először!")
        return

    print("-" * 80)
    print(f"{'ID':<5} | {'HELYES SZÓ':<15} | {'SZINT':<6} | {'MI ELÍRÁSOK (DISTRACTORS)':<40}")
    print("-" * 80)

    for word in results:
        # Mivel egy szóhoz egy quiz_data tartozik (a mi logikánkban)
        quiz = word.quiz_data[0] 
        distractors = f"{quiz.distractor_1}, {quiz.distractor_2}, {quiz.distractor_3}"
        
        print(f"{word.id:<5} | {word.correct:<15} | {word.cefr_level:<6} | {distractors:<40}")

    print("-" * 80)
    print(f"Összesen {len(results)} kész feladatot találtam.")
    
    session.close()

if __name__ == "__main__":
    check_database()