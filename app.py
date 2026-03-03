"""
SPELLING PATTERN CLASSIFIER API
Flask server for your Android spelling quiz app

To run:
    python api_server.py

Your Android app will connect to this to get ML predictions.
"""

from flask import Flask, request, jsonify
import pickle
import pandas as pd

app = Flask(__name__)

# Load model at startup
print("="*60)
print("LOADING ML MODEL...")
print("="*60)

try:
    with open('pattern_classifier_rf.pkl', 'rb') as f:
        model = pickle.load(f)
    with open('feature_names.pkl', 'rb') as f:
        feature_names = pickle.load(f)
    print("✅ Model loaded successfully!")
except Exception as e:
    print(f"❌ ERROR loading model: {e}")
    print("Make sure pattern_classifier_rf.pkl and feature_names.pkl are in the same folder!")
    exit(1)

def extract_features(correct, misspelled):
    """Extract features from a word pair (same as training)"""
    features = {}
    vowels = set('aeiou')
    consonants = set('bcdfghjklmnpqrstvwxyz')
    
    correct = correct.lower()
    misspelled = misspelled.lower()
    
    features['word_length'] = len(correct)
    features['length_diff'] = len(misspelled) - len(correct)
    features['length_same'] = 1 if len(correct) == len(misspelled) else 0
    features['length_shorter'] = 1 if len(misspelled) < len(correct) else 0
    features['length_longer'] = 1 if len(misspelled) > len(correct) else 0
    
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
    
    if len(correct) == len(misspelled):
        diff_positions = [i for i in range(len(correct)) if correct[i] != misspelled[i]]
        if diff_positions:
            features['change_at_start'] = 1 if diff_positions[0] < 3 else 0
            features['change_at_end'] = 1 if diff_positions[-1] > len(correct) - 3 else 0
            features['change_in_middle'] = 1 if 3 <= diff_positions[0] <= len(correct)-3 else 0
            features['num_positions_changed'] = len(diff_positions)
            
            if len(diff_positions) == 2 and diff_positions[1] - diff_positions[0] == 1:
                features['adjacent_swap'] = 1
            else:
                features['adjacent_swap'] = 0
                
            vowel_to_vowel = sum(1 for i in diff_positions 
                                if correct[i] in vowels and misspelled[i] in vowels)
            consonant_to_consonant = sum(1 for i in diff_positions 
                                        if correct[i] in consonants and misspelled[i] in consonants)
            features['vowel_to_vowel_swap'] = vowel_to_vowel
            features['consonant_to_consonant_swap'] = consonant_to_consonant
        else:
            features['change_at_start'] = 0
            features['change_at_end'] = 0
            features['change_in_middle'] = 0
            features['num_positions_changed'] = 0
            features['adjacent_swap'] = 0
            features['vowel_to_vowel_swap'] = 0
            features['consonant_to_consonant_swap'] = 0
    else:
        features['change_at_start'] = 0
        features['change_at_end'] = 0
        features['change_in_middle'] = 0
        features['num_positions_changed'] = 0
        features['adjacent_swap'] = 0
        features['vowel_to_vowel_swap'] = 0
        features['consonant_to_consonant_swap'] = 0
    
    features['has_y_and_i'] = 1 if ('y' in correct and 'i' in correct) else 0
    
    return features

@app.route('/predict', methods=['POST'])
def predict():
    """Predict the mistake pattern for a single word"""
    try:
        data = request.get_json()
        correct = data.get('correct', '').strip()
        wrong = data.get('wrong', '').strip()
        
        if not correct or not wrong:
            return jsonify({'error': 'Missing correct or wrong parameter'}), 400
        
        features = extract_features(correct, wrong)
        features_df = pd.DataFrame([features])
        prediction = model.predict(features_df)[0]
        probabilities = model.predict_proba(features_df)[0]
        confidence = float(max(probabilities))
        
        print(f"✓ {correct} → {wrong} = {prediction} ({confidence:.2f})")
        
        return jsonify({
            'pattern': prediction,
            'confidence': confidence
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/analyze_user', methods=['POST'])
def analyze_user():
    """Analyze multiple mistakes to find weak pattern"""
    try:
        data = request.get_json()
        mistakes = data.get('mistakes', [])
        
        if not mistakes:
            return jsonify({'error': 'No mistakes provided'}), 400
        
        predictions = []
        for mistake in mistakes:
            correct = mistake.get('correct', '').strip()
            wrong = mistake.get('wrong', '').strip()
            if correct and wrong:
                features = extract_features(correct, wrong)
                features_df = pd.DataFrame([features])
                pred = model.predict(features_df)[0]
                predictions.append(pred)
        
        if not predictions:
            return jsonify({'error': 'No valid mistakes'}), 400
        
        from collections import Counter
        pattern_counts = Counter(predictions)
        weak_pattern = pattern_counts.most_common(1)[0][0]
        
        print(f"✓ Analyzed {len(predictions)} mistakes → {weak_pattern}")
        
        return jsonify({
            'weak_pattern': weak_pattern,
            'pattern_counts': dict(pattern_counts)
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    """Health check"""
    return jsonify({'status': 'ok'})

if __name__ == '__main__':
    print("\n" + "="*60)
    print("SPELLING PATTERN API - RUNNING")
    print("="*60)
    print("\n✓ http://localhost:5000")
    print("✓ Android emulator: http://10.0.2.2:5000")
    print("\nPress Ctrl+C to stop\n")
    
    app.run(host='0.0.0.0', port=5000, debug=True)