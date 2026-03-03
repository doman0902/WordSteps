import pandas as pd
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense, Embedding, Dropout
import pickle
import os
from datetime import datetime

# --- 1. ADATOK BETÖLTÉSE SÚLYOZÁSSAL ---
def load_all_data():
    input_texts, target_texts = [], []
    
    # A) Alap adatok
    main_path = 'spelling_training_data_humanlike.csv'
    if os.path.exists(main_path):
        print(f"Alap adatok betöltése: {main_path}...")
        df_main = pd.read_csv(main_path)
        for _, row in df_main.iterrows():
            correct = str(row['correct']).lower().strip()
            mis = str(row['misspelled']).lower().strip().split()
            for m in mis:
                if m != correct and len(m) > 1:
                    input_texts.append(correct)
                    target_texts.append('\t' + m + '\n')
    
    # B) SAJÁT GYŰJTÉS (Most 30x súlyozással, hogy még jobban figyeljen rá!)
    custom_path = 'spelling_mistakes_categorized.csv'
    if os.path.exists(custom_path):
        print(f"Saját gyűjtésű adatok betöltése (30x súlyozással): {custom_path}...")
        df_custom = pd.read_csv(custom_path, sep=';', skiprows=2)
        
        for _, row in df_custom.iterrows():
            if pd.isna(row.iloc[0]) or pd.isna(row.iloc[1]): continue
            correct = str(row.iloc[0]).lower().strip()
            misspelled = str(row.iloc[1]).lower().strip()
            
            if misspelled != correct and len(misspelled) > 1:
                # 30-szor adjuk hozzá a jobb "emlékezés" érdekében
                for _ in range(30):
                    input_texts.append(correct)
                    target_texts.append('\t' + misspelled + '\n')
                
    print(f"Feldolgozás kész. Összesen {len(input_texts)} minta a halmazban.")
    return input_texts, target_texts

# --- 2. ELŐKÉSZÍTÉS ---
input_texts, target_texts = load_all_data()

all_chars = sorted(list(set("".join(input_texts) + "".join(target_texts))))
char_to_int = {char: i for i, char in enumerate(all_chars)}
int_to_char = {i: char for i, char in enumerate(all_chars)}
num_tokens = len(all_chars)
max_encoder_seq_length = max([len(txt) for txt in input_texts])
max_decoder_seq_length = max([len(txt) for txt in target_texts])

encoder_input_data = np.zeros((len(input_texts), max_encoder_seq_length), dtype='float32')
decoder_input_data = np.zeros((len(input_texts), max_decoder_seq_length), dtype='float32')
decoder_target_data = np.zeros((len(input_texts), max_decoder_seq_length, num_tokens), dtype='float32')

for i, (input_text, target_text) in enumerate(zip(input_texts, target_texts)):
    for t, char in enumerate(input_text):
        encoder_input_data[i, t] = char_to_int[char]
    for t, char in enumerate(target_text):
        decoder_input_data[i, t] = char_to_int[char]
        if t > 0:
            decoder_target_data[i, t - 1, char_to_int[char]] = 1.0

# --- 3. ARCHITEKTÚRA ---
latent_dim = 256
encoder_inputs = Input(shape=(None,))
en_x = Embedding(num_tokens, latent_dim)(encoder_inputs)
# Kicsit több dropout a stabilitásért
encoder_lstm = LSTM(latent_dim, return_state=True, dropout=0.3)
_, state_h, state_c = encoder_lstm(en_x)
encoder_states = [state_h, state_c]

decoder_inputs = Input(shape=(None,))
de_x = Embedding(num_tokens, latent_dim)(decoder_inputs)
decoder_lstm = LSTM(latent_dim, return_sequences=True, return_state=True, dropout=0.3)
decoder_outputs, _, _ = decoder_lstm(de_x, initial_state=encoder_states)
decoder_dense = Dense(num_tokens, activation='softmax')
decoder_outputs = decoder_dense(decoder_outputs)

model = Model([encoder_inputs, decoder_inputs], decoder_outputs)
# Kisebb learning rate a finomabb hangoláshoz
optimizer = tf.keras.optimizers.Adam(learning_rate=0.0008)
model.compile(optimizer=optimizer, loss='categorical_crossentropy', metrics=['accuracy'])

# --- 4. TANÍTÁS ---
callback = tf.keras.callbacks.EarlyStopping(monitor='val_loss', patience=12, restore_best_weights=True)

model.fit(
    [encoder_input_data, decoder_input_data], decoder_target_data,
    batch_size=64, epochs=100, validation_split=0.1, callbacks=[callback]
)

# --- 5. MENTÉS (Most már tényleg verziózva!) ---
timestamp = datetime.now().strftime("%m%d_%H%M")
model_file = f'spelling_model_{timestamp}.h5'
meta_file = f'model_metadata_{timestamp}.pkl'

model.save(model_file)
# Emellett elmentjük a "munkapéldányt" is, amit a generátor keres
model.save('spelling_model.h5')

metadata = {
    'char_to_int': char_to_int, 'int_to_char': int_to_char, 
    'max_encoder_seq_length': max_encoder_seq_length, 
    'max_decoder_seq_length': max_decoder_seq_length, 'num_tokens': num_tokens
}

with open(meta_file, 'wb') as f:
    pickle.dump(metadata, f)
with open('model_metadata.pkl', 'wb') as f:
    pickle.dump(metadata, f)

print(f"\nSikeres mentés: {model_file} és spelling_model.h5")