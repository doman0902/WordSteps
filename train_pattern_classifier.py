"""
MISTAKE PATTERN CLASSIFIER
Trains ML to recognize which type of misspelling pattern a user makes.

Your 11 Categories:
1. vowel_swap         - separate→seperate (a→e)
2. single_to_double   - successful→successfull (l→ll)
3. vowel_drop         - beautiful→beutiful (eau→eu)
4. double_to_single   - necessary→necesary (ss→s)
5. insertion          - athlete→athelete (extra e)
6. transposition      - receive→recevie (ei→ie swap position)
7. consonant_drop     - government→goverment (n dropped)
8. consonant_change   - physical→phisical (y→i)
9. ie_ei_swap         - believe→beleive (ie→ei)
10. y_to_ie_ending    - category→categorie (y→ie at end)
11. i_y_swap          - physical→phisycal (i↔y inside word)
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import matplotlib.pyplot as plt
import seaborn as sns
import pickle


def extract_features(correct, misspelled):
    """
    Extract features from a correct→misspelled word pair.
    These features help ML learn the pattern.
    """
    features = {}
    
    # Basic length features
    features['word_length'] = len(correct)
    features['length_diff'] = len(misspelled) - len(correct)
    features['length_same'] = 1 if len(correct) == len(misspelled) else 0
    features['length_shorter'] = 1 if len(misspelled) < len(correct) else 0
    features['length_longer'] = 1 if len(misspelled) > len(correct) else 0
    
    # Character set features
    vowels = set('aeiou')
    consonants = set('bcdfghjklmnpqrstvwxyz')
    
    correct_vowels = sum(1 for c in correct if c in vowels)
    misspelled_vowels = sum(1 for c in misspelled if c in vowels)
    
    features['vowel_count_diff'] = misspelled_vowels - correct_vowels
    features['has_ie'] = 1 if 'ie' in correct else 0
    features['has_ei'] = 1 if 'ei' in correct else 0
    features['has_double_vowel'] = 1 if any(correct[i] == correct[i+1] and correct[i] in vowels 
                                             for i in range(len(correct)-1)) else 0
    features['has_double_consonant'] = 1 if any(correct[i] == correct[i+1] and correct[i] in consonants 
                                                  for i in range(len(correct)-1)) else 0
    features['ends_in_y'] = 1 if correct.endswith('y') else 0
    features['ends_in_e'] = 1 if correct.endswith('e') else 0
    features['ends_in_tely'] = 1 if correct.endswith('tely') else 0
    
    # Position of change features
    if len(correct) == len(misspelled):
        # Same length - find where they differ
        diff_positions = [i for i in range(len(correct)) if correct[i] != misspelled[i]]
        if diff_positions:
            features['change_at_start'] = 1 if diff_positions[0] < 3 else 0
            features['change_at_end'] = 1 if diff_positions[-1] > len(correct) - 3 else 0
            features['change_in_middle'] = 1 if 3 <= diff_positions[0] <= len(correct)-3 else 0
            features['num_positions_changed'] = len(diff_positions)
            
            # Check if adjacent positions changed (transposition)
            if len(diff_positions) == 2 and diff_positions[1] - diff_positions[0] == 1:
                features['adjacent_swap'] = 1
            else:
                features['adjacent_swap'] = 0
        else:
            features['change_at_start'] = 0
            features['change_at_end'] = 0
            features['change_in_middle'] = 0
            features['num_positions_changed'] = 0
            features['adjacent_swap'] = 0
    else:
        features['change_at_start'] = 0
        features['change_at_end'] = 0
        features['change_in_middle'] = 0
        features['num_positions_changed'] = 0
        features['adjacent_swap'] = 0
    
    # Character type change
    if len(correct) == len(misspelled):
        vowel_to_vowel = 0
        consonant_to_consonant = 0
        for i in range(len(correct)):
            if correct[i] != misspelled[i]:
                if correct[i] in vowels and misspelled[i] in vowels:
                    vowel_to_vowel += 1
                elif correct[i] in consonants and misspelled[i] in consonants:
                    consonant_to_consonant += 1
        features['vowel_to_vowel_swap'] = vowel_to_vowel
        features['consonant_to_consonant_swap'] = consonant_to_consonant
    else:
        features['vowel_to_vowel_swap'] = 0
        features['consonant_to_consonant_swap'] = 0
    
    # Special patterns
    features['has_y_and_i'] = 1 if ('y' in correct and 'i' in correct) else 0
    
    return features

print("="*80)
print("MISTAKE PATTERN CLASSIFIER TRAINING")
print("="*80)

# Load your categorized data
df = pd.read_excel('words_categorized.xlsx')
print(f"\nLoaded {len(df)} words")

# Prepare training data
X_data = []
y_data = []

for _, row in df.iterrows():
    word = row['word']
    
    for i in range(1, 4):
        misspelling = row.get(f'misspelling_{i}', '')
        category = row.get(f'rule_{i}', '')
        
        if pd.notna(misspelling) and pd.notna(category) and misspelling.strip() and category.strip():
            features = extract_features(word, misspelling)
            X_data.append(features)
            y_data.append(category)

# Convert to DataFrame
X = pd.DataFrame(X_data)
y = pd.Series(y_data)

print(f"\nTotal training examples: {len(X)}")
print(f"\nCategory distribution:")
print(y.value_counts())

# TRAIN/TEST SPLIT

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

print(f"\nTraining set: {len(X_train)} examples")
print(f"Test set:     {len(X_test)} examples")

# MODEL 1: DECISION TREE

print("\n" + "="*80)
print("MODEL 1: DECISION TREE")
print("="*80)

dt_model = DecisionTreeClassifier(
    max_depth=10,
    min_samples_split=10,
    min_samples_leaf=5,
    random_state=42
)

dt_model.fit(X_train, y_train)
dt_pred = dt_model.predict(X_test)
dt_accuracy = accuracy_score(y_test, dt_pred)

print(f"\nAccuracy: {dt_accuracy:.3f}")
print("\nClassification Report:")
print(classification_report(y_test, dt_pred))

# Feature importance
feature_importance = pd.DataFrame({
    'feature': X.columns,
    'importance': dt_model.feature_importances_
}).sort_values('importance', ascending=False)

print("\nTop 10 Most Important Features:")
for _, row in feature_importance.head(10).iterrows():
    print(f"  {row['feature']:30s}: {row['importance']:.3f}")

# MODEL 2: RANDOM FOREST

print("\n" + "="*80)
print("MODEL 2: RANDOM FOREST")
print("="*80)

rf_model = RandomForestClassifier(
    n_estimators=100,
    max_depth=15,
    min_samples_split=5,
    min_samples_leaf=2,
    random_state=42,
    n_jobs=-1
)

rf_model.fit(X_train, y_train)
rf_pred = rf_model.predict(X_test)
rf_accuracy = accuracy_score(y_test, rf_pred)

print(f"\nAccuracy: {rf_accuracy:.3f}")
print("\nClassification Report:")
print(classification_report(y_test, rf_pred))

# Feature importance
feature_importance_rf = pd.DataFrame({
    'feature': X.columns,
    'importance': rf_model.feature_importances_
}).sort_values('importance', ascending=False)

print("\nTop 10 Most Important Features:")
for _, row in feature_importance_rf.head(10).iterrows():
    print(f"  {row['feature']:30s}: {row['importance']:.3f}")


print("\n" + "="*80)
print("CONFUSION MATRIX (Random Forest)")
print("="*80)

cm = confusion_matrix(y_test, rf_pred)
categories = sorted(y.unique())

# Create confusion matrix plot
plt.figure(figsize=(12, 10))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=categories, yticklabels=categories)
plt.title('Confusion Matrix - Mistake Pattern Classifier')
plt.ylabel('True Category')
plt.xlabel('Predicted Category')
plt.xticks(rotation=45, ha='right')
plt.yticks(rotation=0)
plt.tight_layout()
plt.savefig('confusion_matrix.png', dpi=150, bbox_inches='tight')
print("\n✅ Saved confusion matrix to: confusion_matrix.png")

# Save the best model (Random Forest)
with open('pattern_classifier_rf.pkl', 'wb') as f:
    pickle.dump(rf_model, f)
print("\n✅ Saved Random Forest model to: pattern_classifier_rf.pkl")

# Save Decision Tree too (for explainability)
with open('pattern_classifier_dt.pkl', 'wb') as f:
    pickle.dump(dt_model, f)
print("✅ Saved Decision Tree model to: pattern_classifier_dt.pkl")

# Save feature names for later use
with open('feature_names.pkl', 'wb') as f:
    pickle.dump(X.columns.tolist(), f)
print("✅ Saved feature names to: feature_names.pkl")

# TEST WITH EXAMPLE USER

print("\n" + "="*80)
print("EXAMPLE: USER MISTAKE TRACKING")
print("="*80)

# Simulate a user making mistakes
user_history = [
    ('separate', 'seperate'),      # vowel_swap
    ('receive', 'recieve'),        # ie_ei_swap
    ('necessary', 'necesary'),     # double_to_single
    ('definitely', 'definately'),  # vowel_swap
    ('believe', 'beleive'),        # ie_ei_swap
]

print("\nUser made these mistakes:")
user_predictions = []
for correct, wrong in user_history:
    features = extract_features(correct, wrong)
    features_df = pd.DataFrame([features])
    prediction = rf_model.predict(features_df)[0]
    user_predictions.append(prediction)
    print(f"  {correct:15s} → {wrong:15s}  Predicted: {prediction}")

# Identify user's weak pattern
from collections import Counter
weak_pattern = Counter(user_predictions).most_common(1)[0][0]
print(f"\n User's WEAK PATTERN: {weak_pattern}")
print(f"   → Recommend practicing words with this pattern!")


print("\n" + "="*80)
print("TRAINING COMPLETE!")
print("="*80)
print(f"\nDecision Tree Accuracy:  {dt_accuracy:.1%}")
print(f"Random Forest Accuracy:  {rf_accuracy:.1%}")
print(f"\nFiles created:")
print("  - pattern_classifier_rf.pkl  (best model)")
print("  - pattern_classifier_dt.pkl  (explainable model)")
print("  - feature_names.pkl")
print("  - confusion_matrix.png")
