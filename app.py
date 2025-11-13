import fastapi
import uvicorn
from pydantic import BaseModel
from typing import List
import random

app = fastapi.FastAPI()

class QuizWordResponse(BaseModel):
    id:int
    cefr_level: str
    correct: str
    options: List[str]


@app.get("/test")
async def test_endpoint():
    print("Kotlin hívás beérkezett")
    return {"message": "Python backend válaszolt"}

@app.get("/quiz_word", response_model=QuizWordResponse)
def get_quiz_word():
    correct="elephant"
    distractors=["elefant","elaphant"]
    options=[correct]+distractors
    random.shuffle(options)

    response_data = QuizWordResponse(
        id=1,
        correct=correct,
        cefr_level="A2",
        options=options
    )
    print("Kvíz szó küldve:", response_data.correct)
    return response_data

if __name__ == "__main__":
    print("Teszt indul a http://localhost:8000/test címen...")
    print("Kvíz szó elérhető a http://localhost:8000/quiz_word címen...")
    uvicorn.run(app, host="0.0.0.0", port=8000)