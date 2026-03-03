import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
import pandas as pd
import string
import random

# -----------------------------
# 2) Character tokenizer
# -----------------------------

special_tokens = ["<pad>", "<s>", "</s>"]
alphabet = list(string.ascii_lowercase)
tokens = special_tokens + alphabet

stoi = {ch:i for i,ch in enumerate(tokens)}
itos = {i:ch for ch,i in stoi.items()}

PAD, SOS, EOS = 0, 1, 2

MAX_LEN = 24

def clean_word(word):
    return "".join([c for c in str(word).lower() if c in stoi])

def encode(word):
    word = clean_word(word)
    seq = [SOS] + [stoi[c] for c in word] + [EOS]
    return seq[:MAX_LEN]

def decode(seq):
    return "".join([itos[i] for i in seq if i > 2])


# -----------------------------
# 3) Dataset
# -----------------------------

class SpellingDataset(Dataset):
    def __init__(self, csv_file):
        self.df = pd.read_csv(csv_file)

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        x = encode(self.df.iloc[idx, 0])
        y = encode(self.df.iloc[idx, 1])
        return torch.tensor(x), torch.tensor(y)


# -----------------------------
# 4) Transformer model
# -----------------------------

class TransformerSeq2Seq(nn.Module):
    def __init__(self, vocab_size, dim=256, heads=4, layers=4):
        super().__init__()

        self.embed = nn.Embedding(vocab_size, dim)

        self.transformer = nn.Transformer(
            d_model=dim,
            nhead=heads,
            num_encoder_layers=layers,
            num_decoder_layers=layers,
            batch_first=True
        )

        self.fc = nn.Linear(dim, vocab_size)

    def forward(self, src, tgt):
        src = self.embed(src)
        tgt = self.embed(tgt)
        out = self.transformer(src, tgt)
        return self.fc(out)


# -----------------------------
# 5) Training loop
# -----------------------------

device = "cuda" if torch.cuda.is_available() else "cpu"

def collate_fn(batch):
    srcs, tgts = zip(*batch)

    srcs = nn.utils.rnn.pad_sequence(srcs, batch_first=True, padding_value=PAD)
    tgts = nn.utils.rnn.pad_sequence(tgts, batch_first=True, padding_value=PAD)

    return srcs, tgts

dataset = SpellingDataset("spelling_training_merged.csv")
loader = DataLoader(dataset, batch_size=128, shuffle=True, drop_last=True, collate_fn=collate_fn)

model = TransformerSeq2Seq(len(stoi)).to(device)

criterion = nn.CrossEntropyLoss(ignore_index=PAD, label_smoothing=0.05)
optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4)

EPOCHS = 15
'''
for epoch in range(EPOCHS):
    model.train()
    total_loss = 0

    for src, tgt in loader:
        src = src.to(device)
        tgt = tgt.to(device)

        optimizer.zero_grad()

        out = model(src, tgt[:, :-1])
        loss = criterion(out.reshape(-1, len(stoi)), tgt[:, 1:].reshape(-1))

        loss.backward()
        optimizer.step()

        total_loss += loss.item()

    print(f"Epoch {epoch+1}/{EPOCHS} | Loss: {total_loss/len(loader):.4f}")
'''

# -----------------------------
# 6) Constrained decoding
# -----------------------------

def edit_distance(a, b):
    dp = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]

    for i in range(len(a) + 1): dp[i][0] = i
    for j in range(len(b) + 1): dp[0][j] = j

    for i in range(1, len(a) + 1):
        for j in range(1, len(b) + 1):
            dp[i][j] = min(
                dp[i-1][j] + 1,
                dp[i][j-1] + 1,
                dp[i-1][j-1] + (a[i-1] != b[j-1])
            )

    return dp[-1][-1]


def generate_typos(model, word, n=3, max_dist=5, max_attempts=100):
    model.eval()
    results = set()
    attempts = 0

    while len(results) < n and attempts < max_attempts:
        attempts += 1

        src = torch.tensor([encode(word)]).to(device)
        tgt = torch.tensor([[SOS]]).to(device)

        for _ in range(len(word) + 3):
            out = model(src, tgt)
            probs = F.softmax(out[:, -1] / 1.1, dim=-1)


            topk = torch.topk(probs, k=10)
            idx = topk.indices[0][torch.randint(0, 10, (1,))]

            tgt = torch.cat([tgt, idx.view(1,1)], dim=1)

            if idx.item() == EOS:
                break

        candidate = decode(tgt[0].tolist())

        if 0 < edit_distance(candidate, word) <= max_dist:
            results.add(candidate)

    return list(results)


# -----------------------------
# 7) Quick test
# -----------------------------

test_words = [
    "accommodation",
    "responsibility",
    "separate",
    "government",
    "adverbial"
]

for w in test_words:
    print(w, "→", generate_typos(model, w))
