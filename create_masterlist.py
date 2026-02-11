import pandas as pd
import os

def create_master():
    print("Szólisták összefésülése indul...")
    
    # 1. Betöltés (kezelve a korábbi kódolási és elválasztó hibákat)
    # Az eredeti b2_words.csv pontosvesszős és latin1
    df_old = pd.read_csv('b2_words.csv', sep=';', encoding='latin1')
    df_old.columns = ['word', 'level'] # Egységesítés
    
    # Az új ENGLISH_CERF_WORDS.csv vesszős
    df_new = pd.read_csv('ENGLISH_CERF_WORDS.csv', encoding='utf-8')
    df_new.columns = ['word', 'level']
    
    # 2. Összevonás
    combined = pd.concat([df_old, df_new], ignore_index=True)
    
    # 3. Tisztítási szabályok
    
    # - Kisbetűsítés és szóközök eltávolítása
    combined['word'] = combined['word'].astype(str).str.lower().str.strip()
    
    # - Csak betűkből álló szavak (kiszűrjük a "mull over", "a.m." típusokat)
    combined = combined[combined['word'].str.isalpha()]
    
    # - Kiszűrjük a túl rövid szavakat (3 betű alatt nem sokat lehet hibázni)
    combined = combined[combined['word'].str.len() > 3]
    
    # - Duplikátumok eltávolítása
    final_list = combined.drop_duplicates(subset=['word'])
    
    # 4. Mentés
    final_list = final_list.sort_values(by='word')
    final_list.to_csv('b2_words_master.csv', index=False)
    
    print(f"Kész! Az új lista {len(final_list)} egyedi szót tartalmaz.")
    print("A fájl elmentve: b2_words_master.csv")

if __name__ == "__main__":
    create_master()