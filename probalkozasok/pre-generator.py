import os
import pickle
import numpy as np
import pandas as pd
import tensorflow as tf
from spellchecker import SpellChecker
import sys
import re
from Levenshtein import editops, distance

# --- KONFIGURÁCIÓ ---
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
INPUT_CSV = 'proba.csv'
OUTPUT_CSV = 'generated_distractors.csv'

# --- MODELLEK BETÖLTÉSE ---
print("MI modellek betöltése...")
ml_model = tf.keras.models.load_model('spelling_model_0211_2213.h5', compile=False)
with open('model_metadata.pkl', 'rb') as f:
    ml_metadata = pickle.load(f)

# Encoder modell kinyerése
embeddings = [l for l in ml_model.layers if "embedding" in l.name.lower()]
lstms = [l for l in ml_model.layers if "lstm" in l.name.lower()]
dec_emb, dec_lstm, dec_dense = embeddings[1], lstms[1], ml_model.layers[-1]

encoder_inputs = ml_model.input[0]
_, state_h_enc, state_c_enc = lstms[0].output
encoder_model = tf.keras.Model(encoder_inputs, [state_h_enc, state_c_enc])

spell = SpellChecker()

# --- 1. KATEGÓRIA MEGHATÁROZÓ (HEURISZTIKA) ---
def get_category(correct, misspelled):
    ops = editops(correct, misspelled)
    vowels = "aeiouy"
    if len(ops) == 2 and ops[0][0] == 'replace' and ops[1][0] == 'replace': return "Transposition"
    for op, i, j in ops:
        if op == 'insert' or op == 'delete':
            char = correct[i] if op == 'delete' and i < len(correct) else misspelled[j]
            if char not in vowels: return "DoubleConsonant"
    for op, i, j in ops:
        if op == 'replace' and correct[i] in vowels and misspelled[j] in vowels: return "VowelSwap"
    return "Other"

# --- 2. MINŐSÉGI SZŰRŐK ---
def is_valid_human_error(correct, cand):
    # Ne legyen túl távol
    if distance(correct, cand) > 2: return False
    # Ne legyen kiejthetetlen (max 3 mássalhangzó egymás mellett)
    if re.search(r'[^aeiouy]{4,}', cand): return False
    # Ne legyen létező szó
    if spell.known([cand]): return False
    return True

def sample(preds, temperature=0.7):
    preds = np.asarray(preds).astype('float64')
    preds = np.log(preds + 1e-7) / temperature
    exp_preds = np.exp(preds)
    preds = exp_preds / np.sum(exp_preds)
    return np.argmax(np.random.multinomial(1, preds, 1))

def generate_candidates(word, temperature=0.7):
    try:
        input_seq = np.zeros((1, ml_metadata['max_encoder_seq_length']))
        for t, char in enumerate(word.lower()):
            if char in ml_metadata['char_to_int']: input_seq[0, t] = ml_metadata['char_to_int'][char]
        
        states_value = encoder_model(tf.constant(input_seq), training=False)
        target_seq = np.zeros((1, 1))
        target_seq[0, 0] = ml_metadata['char_to_int']['\t']
        
        decoded = ""
        for _ in range(ml_metadata['max_decoder_seq_length']):
            xt = dec_emb(tf.constant(target_seq))
            lstm_out, h, c = dec_lstm(xt, initial_state=states_value, training=False)
            output_tokens = dec_dense(lstm_out)
            idx = sample(output_tokens[0, -1, :], temperature=temperature)
            char = ml_metadata['int_to_char'][idx]
            if char == '\n': break
            decoded += char
            target_seq[0, 0] = idx
            states_value = [h, c]
        return decoded.strip()
    except: return None

def process_csv():
    df_input = pd.read_csv(INPUT_CSV)
    if 'word' not in df_input.columns: df_input.columns = ['word'] + list(df_input.columns[1:])
    
    results = []
    total = len(df_input)

    print(f"Generálás indul {total} szóra...\n")

    for i, row in df_input.iterrows():
        correct = str(row['word']).lower().strip()
        candidate_pool = {} # Kategória -> Szó mapping a változatosságért
        
        # Generálunk egy nagyobb adagot, amiből válogatunk
        for attempt in range(150):
            temp = 0.4 if attempt < 50 else 0.8 # Előbb a biztosra megyünk, aztán "kreatívkodunk"
            cand = generate_candidates(correct, temperature=temp)
            
            if cand and cand != correct and is_valid_human_error(correct, cand):
                cat = get_category(correct, cand)
                if cat not in candidate_pool:
                    candidate_pool[cat] = cand
            
            if len(candidate_pool) >= 3: break

        # Kiválogatunk 3 különböző típusú hibát
        d_list = list(candidate_pool.values())
        
        # Ha nincs elég típus, feltöltjük bármilyen maradék jó elírással
        if len(d_list) < 3:
            # Fallback: egyszerű karakter csere ha az MI elbukott
            while len(d_list) < 3: d_list.append("") 

        # Terminál visszajelzés
        status = "✔" if "" not in d_list else "⚠"
        print(f"{status} [{i+1}/{total}] {correct} -> {d_list}")

        results.append({
            'word': correct,
            'distractor_1': d_list[0],
            'distractor_2': d_list[1],
            'distractor_3': d_list[2]
        })

    pd.DataFrame(results).to_csv(OUTPUT_CSV, index=False)
    print(f"\nKÉSZ! Eredmények: {OUTPUT_CSV}")

if __name__ == "__main__":
    process_csv()