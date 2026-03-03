import pandas as pd
import random
import os

# --- A KORÁBBI OKOS LOGIKÁNK (VÁLTOZATLAN) ---
def swap_vowel(word):
    vowels = 'aeiou'
    v_list = [i for i, c in enumerate(word) if c in vowels]
    if not v_list: return word
    idx = random.choice(v_list)
    new_v = random.choice([v for v in vowels if v != word[idx]])
    return word[:idx] + new_v + word[idx+1:]

def omit_double(word):
    for i in range(len(word)-1):
        if word[i] == word[i+1]:
            return word[:i] + word[i+1:]
    return word

def transpose(word):
    if len(word) < 2: return word
    idx = random.randint(0, len(word)-2)
    return word[:idx] + word[idx+1] + word[idx] + word[idx+2:]

def phonetic_sim(word):
    replacements = {'ph': 'f', 's': 'c', 'c': 'k', 'y': 'i', 'gh': '', 'oo': 'u'}
    for key, val in replacements.items():
        if key in word:
            return word.replace(key, val, 1)
    return word + "e" if not word.endswith("e") else word[:-1]

# --- ADATGENERÁLÁS ---
def generate_synthetic_data(input_csv, output_csv):
    print(f"Bemeneti fájl keresése: {input_csv}...")
    
    if not os.path.exists(input_csv):
        print(f"HIBA: A '{input_csv}' nem található!")
        return

    try:
        # --- JAVÍTÁS: Automatikus elválasztó felismerés ---
        # A sep=None és engine='python' megkeresi, hogy , vagy ; van a fájlban
        df = pd.read_csv(input_csv, sep=None, engine='python', encoding='latin1')
        
        print(f"Talált oszlopok: {df.columns.tolist()}")

        # Ha a fejléc valamiért egyben maradt (pl. 'word,level'), szétbontjuk kézzel
        if len(df.columns) == 1 and ',' in df.columns[0]:
            col_name = df.columns[0]
            print(f"Észleltem, hogy az oszlopnevek összevontan szerepelnek: {col_name}")
            # Megpróbáljuk újra beolvasni vesszővel
            df = pd.read_csv(input_csv, sep=',', encoding='latin1')
            print(f"Újraolvasás utáni oszlopok: {df.columns.tolist()}")

        # Megkeressük melyik oszlopban vannak a szavak
        target_col = None
        for col in df.columns:
            if 'word' in col.lower():
                target_col = col
                break
        
        if target_col is None:
            print("HIBA: Nem találok olyan oszlopot, aminek a nevében szerepel a 'word'!")
            return

        print(f"A(z) '{target_col}' oszlopot fogom használni forrásként.")

        # Tisztítás
        df[target_col] = df[target_col].astype(str).str.lower().str.strip()
        words = df[target_col].drop_duplicates().tolist()
        
        training_pairs = []
        strategies = [swap_vowel, omit_double, transpose, phonetic_sim]
        
        print(f"Generálás indul {len(words)} alap szóból...")
        
        for word in words:
            if len(str(word)) < 3 or ' ' in str(word): continue
            
            # Minden szóból generálunk 6 különböző elírást
            for _ in range(6):
                strategy = random.choice(strategies)
                misspelled = strategy(word)
                if misspelled != word:
                    training_pairs.append({'correct': word, 'misspelled': misspelled})
        
        # Mentés
        train_df = pd.DataFrame(training_pairs)
        train_df = train_df.sample(frac=1).reset_index(drop=True)
        train_df.to_csv(output_csv, index=False)
        
        print(f"Siker! Generáltunk {len(train_df)} szópárt a '{output_csv}' fájlba.")
        
    except Exception as e:
        print(f"Hiba történt: {e}")

if __name__ == "__main__":
    # Itt a te aktuális fájlnevedet használjuk
    generate_synthetic_data('b2_words_master.csv', 'spelling_data.csv')