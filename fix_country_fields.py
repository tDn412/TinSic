#!/usr/bin/env python3
"""
Check and fix country field inconsistency in Firebase songs collection
"""

import firebase_admin
from firebase_admin import credentials, firestore

def check_and_fix_countries():
    """Check country values and standardize them"""
    
    # Initialize Firebase
    try:
        cred = credentials.Certificate('tinsic-firebase-adminsdk.json')
        firebase_admin.initialize_app(cred)
        print("[OK] Firebase initialized")
    except Exception as e:
        print(f"[ERROR] {e}")
        return
    
    db = firestore.client()
    
    # Get all songs
    songs_ref = db.collection('songs')
    songs = songs_ref.stream()
    
    country_values = {}
    updates_needed = []
    
    print("\n[CHECK] Scanning songs for country values...")
    for song in songs:
        data = song.to_dict()
        country = data.get('country', 'NOT_SET')
        
        if country not in country_values:
            country_values[country] = []
        
        country_values[country].append({
            'id': song.id,
            'title': data.get('title', 'Unknown'),
            'artist': data.get('artist', 'Unknown')
        })
        
        # Check if needs standardization
        if country in ['US-UK', 'us-uk', 'us_uk', 'USUK']:
            updates_needed.append({
                'id': song.id,
                'title': data.get('title'),
                'old_country': country,
                'new_country': 'US_UK'
            })
        elif country in ['vietnam', 'vn', 'VN']:
            updates_needed.append({
                'id': song.id,
                'title': data.get('title'),
                'old_country': country,
                'new_country': 'Vietnam'
            })
    
    # Display results
    print("\n" + "="*60)
    print("Country Field Analysis:")
    print("="*60)
    for country, songs_list in sorted(country_values.items()):
        print(f"\n[{country}] - {len(songs_list)} songs:")
        for song in songs_list[:5]:  # Show first 5
            try:
                print(f"  - {song['title']} by {song['artist']}")
            except UnicodeEncodeError:
                print(f"  - [Song ID: {song['id']}] (Unicode title)")
        if len(songs_list) > 5:
            print(f"  ... and {len(songs_list) - 5} more")
    
    # Show updates needed
    if updates_needed:
        print("\n" + "="*60)
        print(f"[UPDATE NEEDED] {len(updates_needed)} songs need standardization:")
        print("="*60)
        for update in updates_needed:
            print(f"  {update['title']}: '{update['old_country']}' -> '{update['new_country']}'")
        
        # Ask for confirmation
        response = input("\nApply these updates? (yes/no): ")
        if response.lower() == 'yes':
            print("\n[UPDATING] Standardizing country values...")
            for update in updates_needed:
                try:
                    db.collection('songs').document(update['id']).update({
                        'country': update['new_country']
                    })
                    print(f"  [OK] Updated: {update['title']}")
                except Exception as e:
                    print(f"  [ERROR] Failed {update['title']}: {e}")
            print("\n[SUCCESS] Country fields standardized!")
        else:
            print("\n[CANCELLED] No changes made.")
    else:
        print("\n[OK] All country values are consistent!")

if __name__ == '__main__':
    check_and_fix_countries()
