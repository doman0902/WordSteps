import pandas as pd
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense, Embedding
import pickle
import os

# --- 1. ADATOK BETÖLTÉSE ÉS ELŐKÉSZÍTÉSE ---
def load_and_expand_data(file_path):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"A '{file_path}' nem található!")
    
    print(f"Adatok betöltése: {file_path}...")
    df = pd.read_csv(file_path)
    
    input_texts = []
    target_texts = []
    
    for _, row in df.iterrows():
        correct = str(row['correct']).lower().strip()
        # Kezeljük, ha több elírás van egy cellában szóközzel elválasztva
        misspellings = str(row['misspelled']).lower().strip().split()
        
        for m in misspellings:
            if m != correct and len(m) > 1:
                input_texts.append(correct)
                # \t = start token, \n = end token
                target_texts.append('\t' + m + '\n')
                
    return input_texts, target_texts

input_texts, target_texts = load_and_expand_data('spelling_training_data_077.csv')
print(f"Összesen {len(input_texts)} tanító szópár állt össze.")

# --- 2. KARAKTER-TÉRKÉP LÉTREHOZÁSA ---
all_chars = sorted(list(set("".join(input_texts) + "".join(target_texts))))
char_to_int = {char: i for i, char in enumerate(all_chars)}
int_to_char = {i: char for i, char in enumerate(all_chars)}
num_tokens = len(all_chars)

max_encoder_seq_length = max([len(txt) for txt in input_texts])
max_decoder_seq_length = max([len(txt) for txt in target_texts])

print(f"Egyedi karakterek száma: {num_tokens}")
print(f"Maximális szóhossz: {max_encoder_seq_length}")

# --- 3. VEKTORIZÁLÁS (One-Hot Encoding a céladatokhoz) ---
encoder_input_data = np.zeros((len(input_texts), max_encoder_seq_length), dtype='float32')
decoder_input_data = np.zeros((len(input_texts), max_decoder_seq_length), dtype='float32')
decoder_target_data = np.zeros((len(input_texts), max_decoder_seq_length, num_tokens), dtype='float32')

for i, (input_text, target_text) in enumerate(zip(input_texts, target_texts)):
    for t, char in enumerate(input_text):
        encoder_input_data[i, t] = char_to_int[char]
    for t, char in enumerate(target_text):
        decoder_input_data[i, t] = char_to_int[char]
        if t > 0:
            # A céladat egy karakterrel el van csúsztatva (Teacher Forcing)
            decoder_target_data[i, t - 1, char_to_int[char]] = 1.0

# --- 4. MODELL ARCHITEKTÚRA (Seq2Seq LSTM) ---
latent_dim = 256

# Encoder
encoder_inputs = Input(shape=(None,), name="enc_input")
en_x = Embedding(num_tokens, latent_dim, name="enc_embedding")(encoder_inputs)
encoder_lstm = LSTM(latent_dim, return_state=True, name="enc_lstm")
encoder_outputs, state_h, state_c = encoder_lstm(en_x)
encoder_states = [state_h, state_c]

# Decoder
decoder_inputs = Input(shape=(None,), name="dec_input")
de_x = Embedding(num_tokens, latent_dim, name="dec_embedding")(decoder_inputs)
decoder_lstm = LSTM(latent_dim, return_sequences=True, return_state=True, name="dec_lstm")
decoder_outputs, _, _ = decoder_lstm(de_x, initial_state=encoder_states)
decoder_dense = Dense(num_tokens, activation='softmax', name="dec_dense")
decoder_outputs = decoder_dense(decoder_outputs)

model = Model([encoder_inputs, decoder_inputs], decoder_outputs)
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

# --- 5. TANÍTÁS ---
print("Tanítás indítása...")
model.fit(
    [encoder_input_data, decoder_input_data], 
    decoder_target_data,
    batch_size=64,
    epochs=100, # Az emberi adatokon érdemes többet futtatni
    validation_split=0.1
)

# --- 6. MENTÉS ---
print("Modell és metaadatok mentése...")
model.save('spelling_model.h5')
metadata = {
    'char_to_int': char_to_int, 
    'int_to_char': int_to_char,
    'max_encoder_seq_length': max_encoder_seq_length,
    'max_decoder_seq_length': max_decoder_seq_length,
    'num_tokens': num_tokens
}
with open('model_metadata.pkl', 'wb') as f:
    pickle.dump(metadata, f)

print("KÉSZ! A modell használható a pre-generator.py-hoz.")