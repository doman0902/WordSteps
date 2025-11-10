import fastapi
import uvicorn

app = fastapi.FastAPI()


@app.get("/test")
async def test_endpoint():
    print("Kotlin hívás beérkezett")
    return {"message": "Python backend válaszolt"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)