import os
import sys
import json
import psycopg2
import psycopg2.extras


DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'dbname': os.getenv('DB_NAME', 'postgres'),
    'user': os.getenv('DB_USER', 'postgres'),
    'password': os.getenv('DB_PASSWORD', ''),
    'port': int(os.getenv('DB_PORT', 5432))
}


def get_db_connection():
    return psycopg2.connect(**DB_CONFIG)


def parse_and_insert():
    conn = None
    cur = None
    
    try:
        # Step 1: DB 연결 및 데이터 존재 여부 확인
        print("1. DB 연결 중...")
        conn = get_db_connection()
        cur = conn.cursor()
        
        # 이미 데이터가 있는지 확인
        cur.execute("SELECT COUNT(*) FROM Regions")
        count = cur.fetchone()[0]
        
        if count > 0:
            print(f"✓ 이미 {count}개의 지역 데이터가 존재합니다. 초기화를 스킵합니다.")
            return
        
        # Step 2: GeoJSON 파일 로딩
        print("2. GeoJSON 파일 로딩 중...")
        with open('regions.geojson', 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Step 3: 데이터 파싱
        print("3. 데이터 파싱 중...")
        regions_to_insert = []
        features = data.get('features', [])
        
        for feature in features:
            props = feature.get('properties', {})
            full_name = props.get('adm_nm')
            
            if not full_name:
                continue
            
            parts = full_name.split()
            city = ""
            district = ""
            neighborhood = ""
            
            if len(parts) == 3:
                city = parts[0]
                district = parts[1]
                neighborhood = parts[2]
            elif len(parts) == 2:
                city = parts[0]
                district = ""
                neighborhood = parts[1]
            elif len(parts) >= 4:
                city = parts[0]
                district = f"{parts[1]} {parts[2]}"
                neighborhood = parts[3]
            else:
                continue
            
            regions_to_insert.append((city, district, neighborhood))
        
        # Step 4: DB에 삽입
        print(f"4. DB 삽입 시작 (총 {len(regions_to_insert)} 건)...")
        
        insert_query = """
            INSERT INTO Regions (city, district, neighborhood)
            VALUES (%s, %s, %s)
        """
        
        psycopg2.extras.execute_batch(cur, insert_query, regions_to_insert)
        conn.commit()
        
        print(f"✓ 성공! {len(regions_to_insert)}개의 지역 데이터가 DB에 저장되었습니다.")
        
    except FileNotFoundError as e:
        print(f"✗ 오류: 'regions.geojson' 파일을 찾을 수 없습니다.")
        print(f"  상세: {e}")
        sys.exit(1)
        
    except psycopg2.Error as e:
        print(f"✗ DB 에러 발생: {e}")
        if conn:
            conn.rollback()
        sys.exit(1)
        
    except Exception as e:
        print(f"✗ 예상치 못한 오류 발생: {e}")
        if conn:
            conn.rollback()
        sys.exit(1)
        
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()


if __name__ == "__main__":
    parse_and_insert()
