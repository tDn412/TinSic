# TinSic Song Import Script
# This script imports your 20 songs into Firestore

import firebase_admin
from firebase_admin import credentials, firestore, storage

# Initialize Firebase Admin SDK
cred = credentials.Certificate("tinsic-firebase-adminsdk.json")
firebase_admin.initialize_app(cred, {
    'storageBucket': 'tinsic.firebasestorage.app'
})

db = firestore.client()
bucket = storage.bucket()

# Song data from your spreadsheet
songs_data = [
    {"title": "Lạc Trôi", "artist": "Sơn Tùng M-TP", "genre": "Pop", "file": "lac_troi"},
    {"title": "Mang Tiền Về Cho Mẹ", "artist": "Đen Vâu", "genre": "Rap", "file": "mang_tien_ve_cho_me"},
    {"title": "See Tình", "artist": "Hoàng Thùy Linh", "genre": "Dance", "file": "see_tinh"},
    {"title": "Nàng Thơ", "artist": "Hoàng Dũng", "genre": "Ballad", "file": "nang_tho"},
    {"title": "Thằng Điên", "artist": "JustaTee", "genre": "R&B", "file": "thang_dien"},
    {"title": "Em Của Ngày Hôm Qua", "artist": "Sơn Tùng M-TP", "genre": "Pop", "file": "em_cua_ngay_hom_qua"},
    {"title": "Gieo Quẻ", "artist": "Hoàng Thùy Linh", "genre": "Pop", "file": "gieo_que"},
    {"title": "Trốn Tìm", "artist": "Đen", "genre": "Rap", "file": "tron_tim"},
    {"title": "Có Chàng Trai Viết Lên Cây", "artist": "Phan Mạnh Quỳnh", "genre": "Ballad", "file": "co_chang_trai_viet_len_cay"},
    {"title": "Bigcityboy", "artist": "Binz", "genre": "Hip-hop", "file": "bigcityboy"},
    {"title": "Shape of You", "artist": "Ed Sheeran", "genre": "Pop", "file": "shape_of_you"},
    {"title": "Blinding Lights", "artist": "The Weeknd", "genre": "Pop", "file": "blinding_lights"},
    {"title": "Bad Guy", "artist": "Billie Eilish", "genre": "Pop", "file": "bad_guy"},
    {"title": "Believer", "artist": "Imagine Dragons", "genre": "Rock", "file": "believer"},
    {"title": "Levitating", "artist": "Dua Lipa", "genre": "Disco", "file": "levitating"},
    {"title": "Attention", "artist": "Charlie Puth", "genre": "Pop", "file": "attention"},
    {"title": "Counting Stars", "artist": "OneRepublic", "genre": "Rock", "file": "counting_stars"},
    {"title": "Sprinter", "artist": "Central Cee", "genre": "Rap", "file": "sprinter"},
    {"title": "Luther", "artist": "Kendrick Lamar", "genre": "R&B", "file": "luther"},
    {"title": "Sugar", "artist": "Maroon 5", "genre": "Pop", "file": "sugar"},
]

def get_storage_url(file_path):
    """Generate Firebase Storage download URL"""
    blob = bucket.blob(file_path)
    blob.make_public()
    return blob.public_url

def import_songs():
    """Import all songs to Firestore"""
    print("Starting song import to Firestore...")
    
    for idx, song in enumerate(songs_data, 1):
        try:
            # Generate Storage URLs
            audio_url = f"https://firebasestorage.googleapis.com/v0/b/tinsic.firebasestorage.app/o/songs%2F{song['file']}.mp3?alt=media"
            cover_url = f"https://firebasestorage.googleapis.com/v0/b/tinsic.firebasestorage.app/o/covers%2F{song['file']}.jpg?alt=media"
            lyric_url = f"https://firebasestorage.googleapis.com/v0/b/tinsic.firebasestorage.app/o/lyrics%2F{song['file']}.lrc?alt=media"
            
            # Create song document
            song_doc = {
                "title": song["title"],
                "artist": song["artist"],
                "genre": song["genre"],
                "audioUrl": audio_url,
                "coverUrl": cover_url,
                "lyricUrl": lyric_url,
                "duration": 180000  # Default 3 minutes (you can update this later)
            }
            
            # Add to Firestore
            db.collection("songs").add(song_doc)
            print(f"✓ {idx}/20: Added '{song['title']}' by {song['artist']}")
            
        except Exception as e:
            print(f"✗ Error adding '{song['title']}': {str(e)}")
    
    print("\n✅ Import complete! Check your Firestore 'songs' collection.")

if __name__ == "__main__":
    import_songs()
