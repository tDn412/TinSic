#!/usr/bin/env python3
"""
Upload Achievement Definitions to Firebase Firestore

This script uploads achievement data from achievements_firestore_seed.json
to the Firestore collection 'achievements'.

Usage:
    python upload_achievements.py

Requirements:
    - Firebase Admin SDK credentials (serviceAccountKey.json)
    - firebase-admin package (pip install firebase-admin)
"""

import json
import firebase_admin
from firebase_admin import credentials, firestore

def upload_achievements():
    """Upload achievement definitions to Firestore"""
    
    # Initialize Firebase Admin SDK
    try:
        cred = credentials.Certificate('tinsic-firebase-adminsdk.json')
        firebase_admin.initialize_app(cred)
        print("[OK] Firebase Admin SDK initialized successfully")
    except Exception as e:
        print(f"[ERROR] Error initializing Firebase: {e}")
        print("Make sure tinsic-firebase-adminsdk.json exists in the project root")
        return
    
    # Get Firestore client
    db = firestore.client()
    
    # Load achievement data from JSON
    try:
        with open('achievements_firestore_seed.json', 'r', encoding='utf-8') as f:
            data = json.load(f)
            achievements = data['achievements']
        print(f"[OK] Loaded {len(achievements)} achievements from JSON")
    except FileNotFoundError:
        print("[ERROR] achievements_firestore_seed.json not found")
        return
    except json.JSONDecodeError as e:
        print(f"[ERROR] Error parsing JSON: {e}")
        return
    
    # Upload each achievement to Firestore
    success_count = 0
    error_count = 0
    
    print("\n[UPLOAD] Uploading achievements to Firestore...")
    for achievement_id, achievement_data in achievements.items():
        try:
            # Upload to achievements collection with ID as document ID
            db.collection('achievements').document(achievement_id).set(achievement_data)
            print(f"  [OK] {achievement_id}: {achievement_data.get('titleRes', 'N/A')}")
            success_count += 1
        except Exception as e:
            print(f"  [ERROR] {achievement_id}: {e}")
            error_count += 1
    
    # Summary
    print(f"\n{'='*50}")
    print(f"Upload Summary:")
    print(f"  [OK] Success: {success_count}")
    print(f"  [ERROR] Failed: {error_count}")
    print(f"  [TOTAL] Total: {len(achievements)}")
    print(f"{'='*50}")
    
    if success_count > 0:
        print("\n[SUCCESS] Achievements uploaded successfully!")
        print("You can now view them in Firebase Console:")
        print("   => Firestore Database => achievements collection")
    
if __name__ == '__main__':
    upload_achievements()
