import pandas as pd
import re
import os
from spellchecker import SpellChecker

def clean_and_format_data(file_list, output_name):
    """
    Beolvassa a megadott CSV fájlokat, szétbontja a variációkat,
    alkalmazza a minőségi szűrőket és elmenti a végeredményt.
    """
    spell = SpellChecker()
    processed_data = []
    
    # RegEx minta 3 azonos karakter egymás után (pl. sss, lll)
    triple_char_pattern = re.compile(r'(.)\1\1')
    
    print(f"\n--- '{output_name}' feldolgozása indult ---")
    
    for file_path in file_list:
        if not os.path.exists(file_path):
            print(f"Figyelem: A '{file_path}' nem található, kihagyom.")
            continue
            
        print(f"Beolvasás: {file_path}...")
        # Beolvasás (rugalmasan kezeljük a fejlécet: Correct Word / Incorrect Variants)
        df = pd.read_csv(file_path)
        
        # Oszlopnevek egységesítése belső használatra
        # Feltételezzük, hogy az 1. oszlop a helyes, a 2. az elírások listája
        correct_col = df.columns[0]
        misspell_col = df.columns[1]
        
        for _, row in df.iterrows():
            correct = str(row[correct_col]).lower().strip()
            
            # Elírások szétbontása (lehet szóköz vagy vessző az elválasztó)
            misspellings_raw = str(row[misspell_col]).lower().replace(',', ' ')
            misspellings_list = misspellings_raw.split()
            
            for m in misspellings_list:
                m = m.strip()
                
                # --- SZŰRŐK (a korábbi kéréseid alapján) ---
                # 1. Ne legyen ugyanaz
                if m == correct: continue
                # 2. Minimum 3 karakter
                if len(m) < 3: continue
                # 3. Ne legyen tripla betű (pl. adddress)
                if triple_char_pattern.search(m): continue
                # 4. Ne legyen értelmes angol szó (pl. see -> sea)
                if spell.known([m]): continue
                
                processed_data.append({'correct': correct, 'misspelled': m})

    # Duplikátumok eltávolítása
    final_df = pd.DataFrame(processed_data).drop_duplicates()
    final_df.to_csv(output_name, index=False)
    print(f"Siker! '{output_name}' elmentve. Összesen {len(final_df)} példa.")

if __name__ == "__main__":
    # Felosztás a kérésed alapján:
    
    # TANÍTÓ ADATOK (Wikipedia, Birkbeck, Aspell)
    training_files = [
        'wikipedia.csv',
        'birkbeck.csv',
        'aspell.csv'
    ]
    
    # TESZT ADATOK (Testset 1 és 2)
    test_files = [
        'spell-testset1.csv',
        'spell-testset2.csv'
    ]
    
    # Futtatás
    clean_and_format_data(training_files, 'spelling_training_data_077')
    clean_and_format_data(test_files, 'spelling_test_data.csv')

    print("\n--- ÖSSZEGZÉS ---")
    print("Most már használhatod a 'spelling_training_data_077.csv'-t a modell tanításához")
    print("és a 'spelling_test_data.csv'-t a végső kiértékeléshez.")