from flask import Flask, request, jsonify
from flask_cors import CORS
import base64
import random
import time
from datetime import datetime

app = Flask(__name__)
CORS(app)

def mock_face_analysis(image_data: str):
    """Simulate ML facial analysis (symmetry, pallor, tension, etc.)"""
    length = len(image_data)
    symmetry_score = random.uniform(0.6, 1.0)
    pallor_detected = random.random() < 0.2
    tension_detected = random.random() < 0.3

    print(f"[ML] Simulated symmetry score: {symmetry_score:.2f}")
    print(f"[ML] Pallor: {pallor_detected}, Tension: {tension_detected}")

    # Pilih hasil berdasarkan probabilitas
    if symmetry_score < 0.7:
        return "asymmetry"
    elif pallor_detected:
        return "pallor"
    elif tension_detected:
        return "stress"
    else:
        return "healthy"

# ==============================================
# Multilingual Health Condition Data
# ==============================================
HEALTH_CONDITIONS = {
    "healthy": {
        "en": {
            "message": "Your facial analysis looks great! No concerning signs detected.",
            "conditions": [],
            "recommendations": [
                "Maintain a healthy lifestyle",
                "Regular checkups recommended",
                "Stay hydrated"
            ]
        },
        "id": {
            "message": "Analisis wajah Anda terlihat baik! Tidak ada tanda-tanda mengkhawatirkan yang terdeteksi.",
            "conditions": [],
            "recommendations": [
                "Pertahankan gaya hidup sehat",
                "Disarankan pemeriksaan rutin",
                "Tetap terhidrasi"
            ]
        },
        "confidence": 0.95,
        "healthy": True
    },
    "asymmetry": {
        "en": {
            "message": "Facial asymmetry detected. This may indicate potential neurological concerns.",
            "conditions": ["Possible facial paralysis signs", "Asymmetric features detected"],
            "recommendations": [
                "Consult a neurologist immediately",
                "Monitor for other stroke symptoms",
                "Seek medical attention if symptoms worsen"
            ]
        },
        "id": {
            "message": "Asimetri wajah terdeteksi. Hal ini dapat mengindikasikan kemungkinan gangguan saraf.",
            "conditions": ["Kemungkinan tanda kelumpuhan wajah", "Fitur wajah tidak simetris"],
            "recommendations": [
                "Segera konsultasikan ke dokter saraf",
                "Perhatikan gejala stroke lainnya",
                "Segera cari bantuan medis jika memburuk"
            ]
        },
        "confidence": 0.78,
        "healthy": False
    },
    "stress": {
        "en": {
            "message": "Elevated stress indicators detected in facial analysis.",
            "conditions": ["High stress markers", "Facial tension patterns"],
            "recommendations": [
                "Consider stress management techniques",
                "Ensure adequate sleep",
                "Consult healthcare provider if persistent"
            ]
        },
        "id": {
            "message": "Indikator stres tinggi terdeteksi pada analisis wajah.",
            "conditions": ["Penanda stres tinggi", "Pola ketegangan pada wajah"],
            "recommendations": [
                "Pertimbangkan teknik manajemen stres",
                "Pastikan tidur yang cukup",
                "Konsultasikan dengan profesional kesehatan jika berlanjut"
            ]
        },
        "confidence": 0.82,
        "healthy": False
    },
    "pallor": {
        "en": {
            "message": "Facial pallor detected. May indicate anemia or circulation issues.",
            "conditions": ["Reduced facial coloration", "Possible anemia indicators"],
            "recommendations": [
                "Check iron levels with blood test",
                "Increase iron-rich foods in diet",
                "Consult physician for proper diagnosis"
            ]
        },
        "id": {
            "message": "Pucat pada wajah terdeteksi. Dapat mengindikasikan anemia atau masalah sirkulasi.",
            "conditions": ["Warna wajah berkurang", "Kemungkinan tanda anemia"],
            "recommendations": [
                "Periksa kadar zat besi dengan tes darah",
                "Perbanyak makanan kaya zat besi",
                "Konsultasikan dengan dokter untuk diagnosis yang tepat"
            ]
        },
        "confidence": 0.73,
        "healthy": False
    }
}

# ==============================================
# Routes
# ==============================================
@app.route('/api/face-scan', methods=['POST'])
def face_scan():
    try:
        data = request.get_json()
        
        if not data or 'image' not in data:
            return jsonify({
                'success': False,
                'message': 'No image data provided'
            }), 400
        
        image_data = data.get('image')
        language = data.get('language', 'en')
        timestamp = data.get('timestamp', int(time.time() * 1000))
        
        if language not in ['en', 'id']:
            language = 'en'
        
        print(f"Received face scan at {datetime.fromtimestamp(timestamp/1000)} | Lang: {language}")
        print(f"Image data length: {len(image_data)} characters")
        
        # Simulate ML processing delay
        time.sleep(1)
        
        # Use mock ML logic to select condition
        detected_key = mock_face_analysis(image_data)
        result = HEALTH_CONDITIONS.get(detected_key, HEALTH_CONDITIONS["healthy"])
        
        lang_data = result[language]
        
        response = {
            'success': True,
            'message': lang_data['message'],
            'healthy': result['healthy'],
            'conditions': lang_data['conditions'],
            'confidence': result['confidence'],
            'recommendations': lang_data['recommendations'],
            'scan_id': f"SCAN_{timestamp}",
            'language': language,
            'processed_at': datetime.now().isoformat()
        }
        
        return jsonify(response), 200
        
    except Exception as e:
        print(f"Error processing face scan: {str(e)}")
        return jsonify({
            'success': False,
            'message': f'Error processing scan: {str(e)}'
        }), 500


@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'ElderCare Face Scan API',
        'timestamp': datetime.now().isoformat()
    }), 200


@app.route('/', methods=['GET'])
def home():
    return jsonify({
        'message': 'ElderCare Face Scan API',
        'version': '2.0',
        'endpoints': {
            'POST /api/face-scan': 'Upload face scan for analysis (supports language: en/id)',
            'GET /api/health': 'Health check endpoint'
        }
    }), 200


application = app

# Optional local run
if __name__ == '__main__':
    print("=" * 60)
    print("ElderCare Face Scan API Server v2.0 (Multilingual + Mock ML)")
    print("=" * 60)
    app.run(host='0.0.0.0', port=5000, debug=True)
