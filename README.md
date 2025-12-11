# 🏰 Territory Plugin - 완벽 가이드

[![Version](https://img.shields.io/badge/version-1.0--SNAPSHOT-blue.svg)](https://github.com)
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.4+-green.svg)](https://www.minecraft.net)
[![API](https://img.shields.io/badge/Paper-API-orange.svg)](https://papermc.io)
[![Updated](https://img.shields.io/badge/updated-2025--12--11-brightgreen.svg)](https://github.com)

**Territory Plugin**은 Minecraft Paper 서버를 위한 강력한 영토 점령 및 전쟁 시스템입니다.

> **🆕 최신 업데이트 (2025-12-11)**
> - ✅ 점령석 설치 문제 해결 (60+ 블록 타입 지원)
> - ✅ 콘솔 명령어 지원 (reload, startwar, endwar)
> - ✅ 팀 인식 개선 ("팀없음" 자동 처리)
> - ✅ PlaceholderAPI 성능 최적화

---

## 📋 목차

1. [개요](#-개요)
2. [주요 기능](#-주요-기능)
3. [설치 방법](#-설치-방법)
4. [기본 사용법](#-기본-사용법)
5. [설정 파일](#-설정-파일)
6. [명령어](#-명령어)
7. [권한](#-권한)
8. [BlueMap 연동](#-bluemap-연동)
9. [다국어 지원](#-다국어-지원)
10. [개발자 정보](#-개발자-정보)

---

## 🎯 개요

Territory Plugin은 다음을 제공합니다:
- 🏴 **점령석 시스템**: 청크 기반 영토 점령
- ⚔️ **전쟁 시스템**: 국가 간 전면전
- 🗺️ **BlueMap 연동**: 웹 맵에서 실시간 영토 확인
- 🎨 **완전한 커스터마이징**: 모든 메시지와 색상 변경 가능
- 💰 **경제 시스템**: Vault 연동 업그레이드 비용
- 📊 **통계 시스템**: 국가별 랭킹과 전쟁 이력

---

## ✨ 주요 기능

### 1. 점령석 시스템

#### 점령석 구조
- **크기**: 2x2x2 (8개 블록)
- **재질**: 흑요석 (config에서 변경 가능)
- **위치**: 청크 중앙 (x: 7-8, z: 7-8)
- **높이**: config에서 설정 가능

#### 설치 가능한 블록 (60+ 종류)
```
✅ 자연 지형: 흙, 풀블록, 돌, 자갈, 모래, 점토
✅ 식물: 잔디, 큰 잔디, 양치류, 모든 꽃, 버섯
✅ 액체: 물, 용암
✅ 광물: 화강암, 섬록암, 안산암, 딥슬레이트, 응회암
✅ 네더: 네더랙, 영혼 모래, 진홍색/뒤틀린 나일륨
✅ 엔드: 엔드 돌
✅ 기타: 공기, 눈, 덩굴, 빛 이끼

❌ 설치 불가: 건물 블록, 나무, 광석, 기계, 체스트 등
```

#### 티어 시스템
| 티어 | 반경 | 영역 크기 | 업그레이드 방법 |
|------|------|-----------|----------------|
| Tier I | 1 청크 | 3x3 | 아이템으로 설치 |
| Tier II | 4 청크 | 9x9 | 업그레이드 |
| Tier III | 7 청크 | 15x15 | 업그레이드 |
| Tier IV | 10 청크 | 21x21 | 업그레이드 |
| Tier V | 17 청크 | 35x35 | 업그레이드 |

#### 업그레이드 조건
- ✅ 점령 시간 충족
- ✅ 업그레이드 비용 (Vault)
- ✅ 소유권 확인

#### 업그레이드 방법
1. **방법 1**: 점령석 우클릭 → GUI 오픈
2. **방법 2**: `/territory upgrade` 명령어 (방어 블록이 있어도 가능!)

### 2. 전쟁 시스템

#### 전쟁 선포
1. 전쟁 선포 두루마리 획득 (`/territory scroll`)
2. 두루마리 우클릭
3. 확인 클릭 `[YES]`
4. **10분 카운트다운** 시작
5. 전면전 발생

#### 전쟁 규칙
- **평화 시**: 본인 팀 땅만 상호작용 가능
- **전쟁 시**: 모든 땅에서 상호작용 가능
- **점령석 파괴**: 적 점령석 파괴 시 모든 영토 획득

#### 전쟁 특징
- 🌍 **전면전**: 선포한 국가 vs 전 세계
- ⏰ **10분 준비 시간**: 방어 준비 가능
- 📢 **실시간 알림**: 점령석 파괴 시 국가 멤버 전체 알림
- 📝 **전쟁 이력**: 모든 전쟁 기록 저장

### 3. 영토 보호

#### 보호 시스템
```
┌─────────────────────────────────┐
│ 평화 시                          │
├─────────────────────────────────┤
│ ✅ 본인 팀 땅: 모든 작업 가능    │
│ ❌ 다른 팀 땅: 차단              │
│ ❌ 주인 없는 땅: 차단            │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ 전쟁 시                          │
├─────────────────────────────────┤
│ ✅ 모든 땅: 전투/파괴 가능       │
│ ⚔️ PvP 활성화                   │
│ 💥 점령석 파괴 가능              │
└─────────────────────────────────┘
```

### 4. BlueMap 연동

#### 기능
- 🗺️ **실시간 영토 표시**: 모든 영토가 웹 맵에 표시
- 🎨 **팀별 색상**: `team.yml`에서 설정한 색상 적용
- 📦 **3D 마커**: 높이 100블록의 입체 마커
- 🔄 **자동 업데이트**: 점령/파괴 시 즉시 반영
- 👁️ **투명도**: 30% 투명으로 지형 확인 가능

#### 접속 방법
```
http://서버IP:8100
→ "Territory Claims" 마커 활성화
→ 팀별 영토 확인
```

### 5. 통계 시스템

#### 국가 통계
- 📊 총 영토 (청크 수)
- 🏰 점령석 개수
- ⭐ 최고 티어
- 👥 온라인 멤버 수
- ⚔️ 전쟁 상태
- 🏆 영토 점수 및 순위

#### 랭킹 시스템
```
§6=== 국가 랭킹 (영토 점수) ===
§6🥇 §f대한민국 §7- §e2150 §7(청크: 200, 점령석: 8)
§7🥈 §f일본 §7- §e1850 §7(청크: 156, 점령석: 5)
§c🥉 §f중국 §7- §e1200 §7(청크: 98, 점령석: 3)
```

---

## 🚀 설치 방법

### 1. 필수 요구사항

| 항목 | 버전 | 필수 여부 |
|------|------|-----------|
| Minecraft | 1.20.4+ | ✅ 필수 |
| Paper | Latest | ✅ 필수 |
| LuckPerms | 5.0+ | ✅ 필수 |
| Vault | 1.7+ | ⚠️ 권장 |
| BlueMap | 2.7.4+ | ⚠️ 선택 |
| PlaceholderAPI | Latest | ⚠️ 선택 |

### 2. 설치 단계

```bash
# 1. 플러그인 다운로드
# 2. plugins 폴더에 복사
cp territory_Plugin.jar server/plugins/

# 3. 서버 시작
java -jar paper.jar

# 4. 설정 파일 확인
plugins/territory_Plugin/
├── config.yml
├── team.yml
├── lang.yml
└── items.yml
```

### 3. 초기 설정

#### config.yml
```yaml
# 전쟁 가능 월드 설정
war-worlds:
  - world
  - world_nether

# 점령석 설정
occupation-stone:
  spawn-y-coordinate: 70  # 생성 높이
  block-material: OBSIDIAN  # 블록 종류
```

#### team.yml
```yaml
teams:
  korea:
    id: korea
    display-name: "대한민국"
    luckperms-group: korea  # ⚠️ LuckPerms 그룹과 반드시 일치!
    color: "#FF0000"  # 빨강
    description: "Korean Empire"
```

> **⚠️ 중요**: 
> - `luckperms-group`은 LuckPerms의 실제 그룹명과 **정확히 일치**해야 합니다
> - team.yml에 등록되지 않은 그룹을 가진 플레이어는 자동으로 "팀없음"으로 처리됩니다
> - 플레이어는 `/lp user <플레이어> parent add <그룹>`으로 국가에 할당

#### LuckPerms 그룹 생성
```bash
# 1. 국가 그룹 생성
/lp creategroup korea
/lp creategroup japan
/lp creategroup china

# 2. 플레이어 할당
/lp user Player123 parent add korea
/lp user Player456 parent add japan

# 3. 확인
/lp user Player123 info
```

---

## 💻 기본 사용법

### 플레이어 시작 가이드

#### 1. 점령석 설치
```
1. /territory stone (관리자에게 요청)
2. 원하는 위치에 아이템 설치
3. 청크 중앙에 2x2x2 흑요석 생성
4. 주변 3x3 청크 자동 점령
```

#### 2. 영토 확장
```
1. 점령석 우클릭 또는 /territory upgrade
2. GUI에서 요구사항 확인
   - 점령 시간: 1시간+
   - 비용: $10,000+
3. 다이아몬드 클릭
4. 영토 확장 완료!
```

#### 3. 전쟁 선포
```
1. /territory scroll (관리자에게 요청)
2. 두루마리 우클릭
3. [YES] 클릭
4. 10분 후 전면전!
```

#### 4. 영토 방어
```
1. 점령석 주변에 방어 시설 건설
2. /territory stones로 모든 점령석 위치 확인
3. 전쟁 중 실시간 알림 수신
4. 빠른 방어 대응
```

---

## ⚙️ 설정 파일

### config.yml

```yaml
# 전쟁 가능 월드
war-worlds:
  - world
  - world_nether
  - world_the_end

# 점령석 설정
occupation-stone:
  spawn-y-coordinate: 70
  block-material: OBSIDIAN
  upgrade-cost-enabled: true
  
  # 티어별 업그레이드 요구사항
  upgrade-requirements:
    tier-1-to-2:
      money: 10000
      occupation-time: 3600  # 1시간
    tier-2-to-3:
      money: 25000
      occupation-time: 7200  # 2시간
    tier-3-to-4:
      money: 50000
      occupation-time: 14400  # 4시간
    tier-4-to-5:
      money: 100000
      occupation-time: 28800  # 8시간

# 전쟁 설정
war:
  preparation-time: 600  # 10분
  minimum-duration: 1800  # 30분
  auto-end-time: 0  # 0 = 무제한
  
  # 전쟁 선포 시 실행될 콘솔 명령어
  declaration-commands:
    - "say {team}이(가) 전면전을 선포했습니다!"
    - "title @a title {\"text\":\"전쟁 경고!\",\"color\":\"red\"}"
```

### team.yml

```yaml
teams:
  korea:
    id: korea
    display-name: "대한민국"
    luckperms-group: korea
    color: "#FF0000"
    description: "Korean Empire"
  
  japan:
    id: japan
    display-name: "일본"
    luckperms-group: japan
    color: "#00FF00"
    description: "Japanese Nation"
```

### lang.yml (다국어)

```yaml
# 아이템 이름
items:
  occupation_stone:
    name: "&6Occupation Stone (Tier I)"
    lore:
      - "&7영토를 점령하는 점령석입니다"
      - "&7설치하면 주변 3x3 청크를 점령합니다"

# 메시지
stone:
  placed: "&a점령석을 설치했습니다!"
  upgraded: "&a점령석이 {tier}로 업그레이드되었습니다!"
  destroyed_conquest: "&a점령석을 파괴하고 영토를 점령했습니다!"
```

### items.yml (커스텀 모델)

```yaml
# 커스텀 모델 데이터 (리소스팩 연동)
Occupation_Stone_Tier_I: 1
War_Declaration_Scroll: 2
```

---

## 📜 명령어

### 🔴 관리자 명령어

| 명령어 | 설명 | 권한 | 콘솔 사용 |
|--------|------|------|----------|
| `/territory stone` | Tier I 점령석 지급 | `territory.admin` | ❌ |
| `/territory scroll` | 전쟁 두루마리 지급 | `territory.admin` | ❌ |
| `/territory reload` | 설정 리로드 | `territory.admin` | ✅ |
| `/territory startwar <국가>` | 전쟁 즉시 시작 | `territory.admin` | ✅ |
| `/territory endwar <국가>` | 전쟁 강제 종료 | `territory.admin` | ✅ |

> **💡 콘솔 지원**: `reload`, `startwar`, `endwar` 명령어는 콘솔에서도 사용 가능합니다!

### 🟢 플레이어 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/territory info` | 영토 정보 확인 | 없음 |
| `/territory upgrade` | 업그레이드 GUI | `territory.upgrade` |
| `/territory team` | 팀 목록 | 없음 |
| `/territory stats [팀]` | 국가 통계 | 없음 |
| `/territory ranking` | 국가 랭킹 | 없음 |
| `/territory find` | 가장 가까운 적 점령석 | 없음 |
| `/territory stones [팀]` | 점령석 목록 | 없음 |
| `/territory history [팀]` | 전쟁 이력 | 없음 |
| `/territory score <차수>` | 전쟁 점수 확인 | 없음 |
| `/territory scorenow` | 현재 전쟁 점수 | 없음 |
| `/territory cancel` | 지역 이름 입력 취소 | 없음 |

### 별칭
- `/territory` = `/terr` = `/t`

### 콘솔 명령어 예시
```bash
# 서버 콘솔에서 실행 가능
territory reload
territory startwar korea
territory endwar japan
```

---

## 🔐 권한

### 기본 권한

```yaml
permissions:
  territory.admin:
    description: 관리자 권한
    default: op
  
  territory.upgrade:
    description: 점령석 업그레이드
    default: true
  
  territory.war.declare:
    description: 전쟁 선포
    default: true
```

### LuckPerms 설정 예시

```bash
# 관리자 권한
lp group admin permission set territory.admin true

# 특정 그룹만 전쟁 선포
lp group leader permission set territory.war.declare true
lp group member permission set territory.war.declare false

# 업그레이드 권한 제한
lp group vip permission set territory.upgrade true
lp group default permission set territory.upgrade false
```

---

## 🗺️ BlueMap 연동

### 설정 방법

#### 1. BlueMap 설치
```bash
# BlueMap 다운로드
https://github.com/BlueMap-Minecraft/BlueMap/releases

# plugins 폴더에 추가
cp BlueMap.jar server/plugins/

# 서버 재시작
```

#### 2. Territory Plugin 설치
```bash
# 자동으로 BlueMap 감지 및 연동
[Territory] BlueMap API connected! Territory markers enabled.
```

#### 3. 웹 맵 접속
```
http://서버IP:8100
→ 오른쪽 상단 메뉴
→ "Markers" 또는 "마커"
→ "Territory Claims" 체크
```

### 팀 색상 설정

```yaml
# team.yml
teams:
  korea:
    color: "#FF0000"  # 빨강 (BlueMap에 자동 적용)
  
  japan:
    color: "#00FF00"  # 초록
  
  china:
    color: "#0000FF"  # 파랑
```

### 색상 예시

```yaml
# 빨강 계열
"#FF0000" - 순수 빨강
"#FF6B6B" - 밝은 빨강
"#C0392B" - 진한 빨강

# 파랑 계열
"#0000FF" - 순수 파랑
"#3498DB" - 하늘색
"#2C3E50" - 진한 파랑

# 초록 계열
"#00FF00" - 순수 초록
"#2ECC71" - 민트색
"#27AE60" - 진한 초록

# 기타
"#F1C40F" - 금색
"#9B59B6" - 보라
"#E91E63" - 핫핑크
```

### 작동 방식

```
1. 점령석 설치/업그레이드/파괴
   ↓
2. updateMarkers() 자동 호출
   ↓
3. 모든 청크 데이터 조회
   ↓
4. 팀별로 그룹화
   ↓
5. team.yml 색상 적용
   ↓
6. BlueMap에 3D 마커 생성
   ↓
7. 웹 맵에 실시간 반영
```

---

## 🌐 다국어 지원

### 메시지 변경

```yaml
# lang.yml 편집
stone:
  placed: "&aOccupation stone placed successfully!"
  upgraded: "&aStone upgraded to {tier}!"

# 리로드
/territory reload
```

### 플레이스홀더

| 플레이스홀더 | 설명 | 예시 |
|-------------|------|------|
| `{nation}` | 국가명 | korea |
| `{tier}` | 티어 | TIER_3 |
| `{radius}` | 반경 | 7 |
| `{money}` | 돈 | 10000 |
| `{hours}` | 시간 | 2 |
| `{minutes}` | 분 | 30 |

### 색상 코드

```yaml
&0 - 검정     &8 - 진한 회색
&1 - 진한 파랑 &9 - 파랑
&2 - 진한 초록 &a - 초록
&3 - 진한 청록 &b - 청록
&4 - 진한 빨강 &c - 빨강
&5 - 진한 보라 &d - 분홍
&6 - 금색     &e - 노랑
&7 - 회색     &f - 흰색

&l - 굵게     &o - 기울임
&m - 취소선   &n - 밑줄
```

---

## 🎨 커스텀 모델 데이터

### items.yml 설정

```yaml
# 리소스팩 연동
Occupation_Stone_Tier_I: 100
War_Declaration_Scroll: 200
```

### 리소스팩 예시

```json
// assets/minecraft/models/item/beacon.json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "item/beacon"
  },
  "overrides": [
    {
      "predicate": {
        "custom_model_data": 100
      },
      "model": "item/custom/occupation_stone"
    }
  ]
}
```

---

## 📊 PlaceholderAPI 변수

### 플레이어 관련 변수

| 플레이스홀더 | 설명 | 반환 값 예시 |
|------------|------|------------|
| `%territory_team%` | **플레이어가 속한 팀** | `korea`, `팀없음` |
| `%territory_team_display%` | 플레이어 팀 표시 이름 | `대한민국`, `팀없음` |
| `%territory_team_in_war%` | 플레이어 팀 전쟁 상태 | `예`, `아니오` |
| `%territory_team_war_time_left%` | 전쟁 시작까지 남은 시간 (초) | `300`, `0` |
| `%territory_team_war_time_left_formatted%` | 전쟁 시작까지 남은 시간 (MM:SS) | `05:00`, `00:00` |
| `%territory_owned_chunks%` | 플레이어 팀이 소유한 청크 수 | `156` |

### 위치 관련 변수

| 플레이스홀더 | 설명 | 반환 값 예시 |
|------------|------|------------|
| `%territory_chunk_owner%` | 현재 청크 소유자 | `korea`, `없음` |
| `%territory_chunk_owner_display%` | 현재 청크 소유자 표시 이름 | `대한민국`, `없음` |

### 전역 변수

| 플레이스홀더 | 설명 | 반환 값 예시 |
|------------|------|------------|
| `%territory_war_<팀명>%` | 특정 팀의 전쟁 상태 | `전쟁 중`, `평화` |
| `%territory_warprep_<팀명>%` | 특정 팀의 전쟁 준비 시간 | `05:30`, `없음` |
| `%territory_total_teams%` | 전체 팀 수 | `5` |
| `%territory_teams_at_war%` | 전쟁 중인 팀 수 | `2` |

### 사용 예시

#### 스코어보드
```yaml
# DeluxeScoreboard 예시
scoreboard:
  title: "&6⚔ Territory Info ⚔"
  lines:
    - "&e━━━━━━━━━━━━━━━"
    - "&f내 팀: &b%territory_team_display%"
    - "&f전쟁: %territory_team_in_war%"
    - "&f소유 영토: &a%territory_owned_chunks% &7청크"
    - "&e━━━━━━━━━━━━━━━"
    - "&f현재 위치: %territory_chunk_owner_display%"
    - "&e━━━━━━━━━━━━━━━"
```

#### TAB 메뉴
```yaml
# TAB 플러그인 예시
header:
  - "&6━━━━━━━━━━━━━━━━━━━"
  - "&e⚔ Territory Wars ⚔"
  - "&f당신의 국가: &b%territory_team_display%"
  - "&f전쟁 상태: %territory_team_in_war%"
  - "&6━━━━━━━━━━━━━━━━━━━"

tablist-name: "&b%territory_team% &7| &f%player_name%"
```

#### 채팅 포맷
```yaml
# EssentialsChat 예시
format: "[%territory_team_display%] {DISPLAYNAME}: {MESSAGE}"

# 예시 출력
# [대한민국] Player123: 안녕하세요!
```

#### 홀로그램
```yaml
# DecentHolograms 예시
lines:
  - "&6⚔ 전쟁 현황 ⚔"
  - "&e전쟁 중인 국가: &c%territory_teams_at_war%개"
  - "&e평화로운 국가: &a%territory_total_teams%개"
  - ""
  - "&7실시간 업데이트!"
```

---

## 📜 Skript 연동

Territory Plugin은 **Skript-Reflect**를 통해 완벽하게 연동 가능합니다!

### 필수 요구사항

```
✅ Skript 2.6+
✅ skript-reflect 2.0+
✅ Territory Plugin
```

### 설치 방법

```bash
1. Skript 설치
2. skript-reflect 설치
3. Territory Plugin 설치
4. 서버 재시작
```

---

### 🎯 Skript API 사용 예제

#### 1. 플레이어가 서 있는 위치의 영토 확인

```skript
command /check:
    trigger:
        set {_world} to world of player
        set {_chunkX} to player's x-coordinate divided by 16
        set {_chunkZ} to player's z-coordinate divided by 16
        set {_chunkKey} to "%{_world}%;%floor({_chunkX})%;%floor({_chunkZ})%"
        
        # Territory Plugin API 호출
        set {_plugin} to plugin "territory_Plugin"
        set {_database} to {_plugin}.getDatabaseManager()
        set {_owner} to {_database}.getChunkOwner({_chunkKey})
        
        if {_owner} is set:
            send "§e이 땅은 §a%{_owner}% §e국가 소유입니다!" to player
        else:
            send "§e이 땅은 주인이 없습니다!" to player
```

#### 2. 특정 국가의 영지에 들어갔을 때 메시지

```skript
import:
    kr.skarch.territory_Plugin.Territory_Plugin
    org.bukkit.Bukkit

every 2 seconds:
    loop all players:
        set {_world} to world of loop-player
        set {_chunkX} to x-coordinate of loop-player divided by 16
        set {_chunkZ} to z-coordinate of loop-player divided by 16
        set {_chunkKey} to "%{_world}%;%floor({_chunkX})%;%floor({_chunkZ})%"
        
        # API 호출
        set {_plugin} to Bukkit.getPluginManager().getPlugin("territory_Plugin")
        set {_owner} to {_plugin}.getDatabaseManager().getChunkOwner({_chunkKey})
        
        # 이전 위치와 비교
        if {_owner} is not {territory::%uuid of loop-player%}:
            set {territory::%uuid of loop-player%} to {_owner}
            
            if {_owner} is set:
                send title "§6영토 진입" with subtitle "§e%{_owner}% 국가" to loop-player for 2 seconds
                play sound "block.note_block.pling" to loop-player
            else:
                send title "§7무주지" with subtitle "§f주인 없는 땅" to loop-player for 2 seconds
```

#### 3. 점령석 정보 확인

```skript
command /stoneinfo:
    trigger:
        set {_world} to world of player
        set {_chunkX} to x-coordinate of player divided by 16
        set {_chunkZ} to z-coordinate of player divided by 16
        set {_centerX} to (floor({_chunkX}) * 16) + 7
        set {_centerZ} to (floor({_chunkZ}) * 16) + 7
        set {_y} to 70  # config의 spawn-y-coordinate
        
        # 점령석 위치 계산
        set {_plugin} to plugin "territory_Plugin"
        set {_db} to {_plugin}.getDatabaseManager()
        
        # Location 객체 생성
        set {_loc} to location at {_centerX}, {_y}, {_centerZ} in {_world}
        set {_stone} to {_db}.getStoneByLocation({_loc})
        
        if {_stone} is set:
            set {_tier} to {_stone}.getCurrentTier()
            set {_owner} to {_stone}.getOwnerGroup()
            set {_time} to {_stone}.getOccupationTime()
            
            send "§6=== 점령석 정보 ===" to player
            send "§e소유자: §f%{_owner}%" to player
            send "§e티어: §f%{_tier}%" to player
            send "§e점령 시간: §f%{_time}% 초" to player
        else:
            send "§c이 청크에는 점령석이 없습니다!" to player
```

#### 4. 전쟁 상태 확인

```skript
command /warcheck <text>:
    trigger:
        set {_nation} to arg-1
        set {_plugin} to plugin "territory_Plugin"
        set {_warManager} to {_plugin}.getWarManager()
        set {_isWar} to {_warManager}.isInGlobalWar({_nation})
        
        if {_isWar} is true:
            send "§c%{_nation}%은(는) 현재 전쟁 중입니다!" to player
        else:
            send "§a%{_nation}%은(는) 평화 상태입니다." to player
```

#### 5. 국가 통계 조회

```skript
command /nationstats <text>:
    trigger:
        set {_nation} to arg-1
        set {_plugin} to plugin "territory_Plugin"
        set {_statsManager} to {_plugin}.getStatsManager()
        set {_stats} to {_statsManager}.getNationStats({_nation})
        
        if {_stats} is set:
            set {_chunks} to {_stats}.getTotalChunks()
            set {_stones} to {_stats}.getTotalStones()
            set {_tier} to {_stats}.getHighestTier()
            set {_score} to {_stats}.getTerritoryScore()
            
            send "§6=== %{_nation}% 통계 ===" to player
            send "§e영토: §f%{_chunks}% 청크" to player
            send "§e점령석: §f%{_stones}%개" to player
            send "§e최고 티어: §f%{_tier}%" to player
            send "§e영토 점수: §f%{_score}%" to player
        else:
            send "§c국가 정보를 찾을 수 없습니다!" to player
```

#### 6. 영토 이동 제한 (특정 국가만 입장)

```skript
on region enter:
    set {_world} to world of player
    set {_chunkX} to x-coordinate of player divided by 16
    set {_chunkZ} to z-coordinate of player divided by 16
    set {_chunkKey} to "%{_world}%;%floor({_chunkX})%;%floor({_chunkZ})%"
    
    set {_plugin} to plugin "territory_Plugin"
    set {_owner} to {_plugin}.getDatabaseManager().getChunkOwner({_chunkKey})
    
    # VIP 영역 체크 (예: "vip_nation")
    if {_owner} is "vip_nation":
        if player doesn't have permission "territory.vip":
            send "§c이 영역은 VIP 전용입니다!" to player
            push player backwards at speed 2
            cancel event
```

#### 7. 점령석 설치 이벤트 감지

```skript
# 플레이어가 Beacon을 설치할 때
on place of beacon:
    set {_item} to player's tool
    if name of {_item} contains "Occupation Stone":
        wait 1 tick
        
        # 점령석이 생성되었는지 확인
        set {_world} to world of player
        set {_loc} to location of event-block
        
        send "§a점령석이 설치되었습니다!" to all players
        send "§e위치: %{_world}% (%x of {_loc}%, %y of {_loc}%, %z of {_loc}%)" to all players
```

#### 8. 자동 영토 세금 시스템

```skript
every 1 hour:
    loop all players:
        # 플레이어의 국가 확인
        set {_plugin} to plugin "territory_Plugin"
        set {_playerGroup} to loop-player's primary group
        
        # 국가의 총 영토 확인
        set {_stats} to {_plugin}.getStatsManager().getNationStats({_playerGroup})
        
        if {_stats} is set:
            set {_chunks} to {_stats}.getTotalChunks()
            set {_tax} to {_chunks} * 10  # 청크당 10원
            
            # 경제 플러그인 연동 (Vault)
            if balance of loop-player >= {_tax}:
                remove {_tax} from balance of loop-player
                send "§e영토 세금 §c-%{_tax}%원 §7(청크: %{_chunks}%)" to loop-player
            else:
                send "§c영토 세금을 낼 수 없습니다! 파산 주의!" to loop-player
```

#### 9. 영토 랭킹 표시

```skript
command /ranking:
    trigger:
        set {_plugin} to plugin "territory_Plugin"
        set {_statsManager} to {_plugin}.getStatsManager()
        set {_allStats} to {_statsManager}.getAllNationStats()
        
        send "§6=== 국가 랭킹 ===" to player
        
        set {_rank} to 1
        loop {_allStats}:
            set {_nation} to loop-value.getNationName()
            set {_score} to loop-value.getTerritoryScore()
            set {_chunks} to loop-value.getTotalChunks()
            
            if {_rank} is 1:
                send "§6🥇 %{_nation}% - %{_score}% (%{_chunks}% 청크)" to player
            else if {_rank} is 2:
                send "§7🥈 %{_nation}% - %{_score}% (%{_chunks}% 청크)" to player
            else if {_rank} is 3:
                send "§c🥉 %{_nation}% - %{_score}% (%{_chunks}% 청크)" to player
            else:
                send "§e%{_rank}%. %{_nation}% - %{_score}% (%{_chunks}% 청크)" to player
            
            add 1 to {_rank}
            if {_rank} > 10:
                stop loop
```

#### 10. 점령석 파괴 알림 (Discord 연동)

```skript
# 점령석(흑요석)이 파괴될 때
on break of obsidian:
    set {_loc} to location of event-block
    set {_plugin} to plugin "territory_Plugin"
    set {_db} to {_plugin}.getDatabaseManager()
    
    # 근처 점령석 확인 (2x2x2 구조)
    loop blocks in radius 2 of {_loc}:
        set {_stone} to {_db}.getStoneByLocation(location of loop-block)
        if {_stone} is set:
            set {_owner} to {_stone}.getOwnerGroup()
            set {_breaker} to player's primary group
            
            # Discord 웹훅 (예시)
            send "⚠️ **점령석 파괴!**" to discord webhook "YOUR_WEBHOOK_URL"
            send "피해국: %{_owner}%" to discord webhook "YOUR_WEBHOOK_URL"
            send "공격자: %{_breaker}%" to discord webhook "YOUR_WEBHOOK_URL"
            send "위치: %world of {_loc}% (%x of {_loc}%, %y of {_loc}%, %z of {_loc}%)" to discord webhook "YOUR_WEBHOOK_URL"
            
            stop loop
```

---

### 📚 Territory Plugin API 레퍼런스

#### DatabaseManager

```skript
# 청크 소유자 확인
{_owner} = {_database}.getChunkOwner({_chunkKey})
# 반환: String (국가명) 또는 null

# 점령석 정보 가져오기
{_stone} = {_database}.getStoneByLocation({_location})
# 반환: OccupationStone 또는 null

# 점령석 UUID로 가져오기
{_stone} = {_database}.getStoneByUuid({_uuid})

# 특정 팀의 점령석 목록
{_stones} = {_database}.getStonesByTeam({_teamName})
# 반환: List<OccupationStone>

# 전체 영토 맵
{_territories} = {_database}.getAllTerritories()
# 반환: Map<String, String> (chunkKey -> owner)

# 청크 개수 확인
{_count} = {_database}.getChunkCountByTeam({_teamName})
# 반환: Int
```

#### TerritoryManager

```skript
# 점령석 설치
{_stone} = {_territoryManager}.placeStone({_location}, {_ownerGroup})
# 반환: OccupationStone 또는 null

# 점령석 업그레이드
{_success} = {_territoryManager}.upgradeStone({_stone})
# 반환: Boolean

# 점령석 파괴
{_territoryManager}.destroyStone({_stone}, {_newOwnerGroup})
```

#### WarManager

```skript
# 전쟁 상태 확인
{_isWar} = {_warManager}.isInGlobalWar({_nationName})
# 반환: Boolean

# 전쟁 선포
{_warManager}.declareGlobalWar({_nationName})

# 전쟁 종료
{_warManager}.endGlobalWar({_nationName})

# 교전 가능 여부
{_canFight} = {_warManager}.canEngage({_attacker}, {_defender})
# 반환: Boolean

# 남은 준비 시간
{_timeLeft} = {_warManager}.getTimeUntilWar({_nationName})
# 반환: Long (초)
```

#### StatsManager

```skript
# 국가 통계
{_stats} = {_statsManager}.getNationStats({_nationName})
# 반환: NationStats 또는 null

# 전체 국가 통계 (랭킹순)
{_allStats} = {_statsManager}.getAllNationStats()
# 반환: List<NationStats>

# 국가 순위
{_rank} = {_statsManager}.getNationRanking({_nationName})
# 반환: Int

# 가장 가까운 적 점령석
{_location} = {_statsManager}.findNearestEnemyStone({_playerLocation}, {_playerTeam})
# 반환: Location 또는 null

# 점령석 위치 목록
{_locations} = {_statsManager}.getStoneLocations({_nationName})
# 반환: List<String>
```

#### OccupationStone (객체)

```skript
# 티어 가져오기
{_tier} = {_stone}.getCurrentTier()
# 반환: StoneTier (TIER_1, TIER_2, ...)

# 소유자 가져오기
{_owner} = {_stone}.getOwnerGroup()
# 반환: String

# 위치 가져오기
{_location} = {_stone}.getLocation()
# 반환: Location

# UUID 가져오기
{_uuid} = {_stone}.getStoneUuid()
# 반환: UUID

# 점령 시간 가져오기 (초)
{_time} = {_stone}.getOccupationTime()
# 반환: Long

# 생성 시간 가져오기
{_created} = {_stone}.getCreatedAt()
# 반환: Long (timestamp)
```

#### NationStats (객체)

```skript
# 국가명
{_name} = {_stats}.getNationName()

# 표시명
{_display} = {_stats}.getDisplayName()

# 총 청크 수
{_chunks} = {_stats}.getTotalChunks()

# 점령석 수
{_stones} = {_stats}.getTotalStones()

# 최고 티어
{_tier} = {_stats}.getHighestTier()

# 전쟁 여부
{_war} = {_stats}.isAtWar()

# 온라인 멤버 수
{_members} = {_stats}.getMemberCount()

# 영토 점수
{_score} = {_stats}.getTerritoryScore()
```

---

### 🎮 실전 예제: 완전한 영토 시스템

#### 영토 입장 시스템 (타이틀 + 사운드 + 파티클)

```skript
import:
    kr.skarch.territory_Plugin.Territory_Plugin
    org.bukkit.Bukkit
    org.bukkit.Particle

every 1 second:
    loop all players:
        set {_world} to world of loop-player
        set {_chunkX} to x-coordinate of loop-player divided by 16
        set {_chunkZ} to z-coordinate of loop-player divided by 16
        set {_chunkKey} to "%{_world}%;%floor({_chunkX})%;%floor({_chunkZ})%"
        
        set {_plugin} to Bukkit.getPluginManager().getPlugin("territory_Plugin")
        set {_owner} to {_plugin}.getDatabaseManager().getChunkOwner({_chunkKey})
        
        # 영토 변경 감지
        if {_owner} is not {territory::%uuid of loop-player%}:
            set {territory::%uuid of loop-player%} to {_owner}
            
            if {_owner} is set:
                # 통계 가져오기
                set {_stats} to {_plugin}.getStatsManager().getNationStats({_owner})
                set {_display} to {_stats}.getDisplayName()
                set {_chunks} to {_stats}.getTotalChunks()
                
                # 같은 팀인지 확인
                set {_playerGroup} to loop-player's primary group
                
                if {_playerGroup} is {_owner}:
                    # 본인 영토
                    send title "§a%{_display}%" with subtitle "§7우리 국가 영토" to loop-player for 3 seconds
                    play sound "block.note_block.chime" to loop-player
                    spawn 10 of particle "villager_happy" at loop-player
                else:
                    # 적 영토
                    set {_isWar} to {_plugin}.getWarManager().isInGlobalWar({_owner})
                    
                    if {_isWar} is true:
                        send title "§c%{_display}%" with subtitle "§4⚔ 전쟁 중인 영토! §4⚔" to loop-player for 3 seconds
                        play sound "entity.ender_dragon.growl" to loop-player
                        spawn 20 of particle "lava" at loop-player
                    else:
                        send title "§6%{_display}%" with subtitle "§e영토 (%{_chunks}% 청크)" to loop-player for 3 seconds
                        play sound "block.note_block.pling" to loop-player
                        spawn 10 of particle "end_rod" at loop-player
            else:
                # 무주지
                send title "§7무주지" with subtitle "§f주인 없는 땅 - 점령 가능!" to loop-player for 2 seconds
                play sound "block.note_block.bass" to loop-player
```

#### 영토 보호 시스템

```skript
on break:
    set {_world} to world of player
    set {_chunkX} to x-coordinate of event-block divided by 16
    set {_chunkZ} to z-coordinate of event-block divided by 16
    set {_chunkKey} to "%{_world}%;%floor({_chunkX})%;%floor({_chunkZ})%"
    
    set {_plugin} to plugin "territory_Plugin"
    set {_owner} to {_plugin}.getDatabaseManager().getChunkOwner({_chunkKey})
    
    if {_owner} is set:
        set {_playerGroup} to player's primary group
        
        if {_playerGroup} is not {_owner}:
            # 남의 땅
            set {_isWar} to {_plugin}.getWarManager().isInGlobalWar({_owner})
            
            if {_isWar} is false:
                cancel event
                send "§c이 영토는 %{_owner}% 국가 소유입니다!" to player
                send "§e전쟁을 선포해야 파괴할 수 있습니다." to player
```

---

### 💡 활용 팁

1. **성능 최적화**
   - API 호출을 반복문 밖으로 이동
   - 캐싱 변수 사용 (`{territory::%uuid%}`)

2. **에러 처리**
   ```skript
   if {_plugin} is not set:
       send "Territory Plugin이 로드되지 않았습니다!"
       stop
   ```

3. **비동기 처리**
   - 무거운 작업은 `wait 1 tick` 사용
   - DB 조회는 최소화

4. **Discord/웹훅 연동**
   - skript-webhook 애드온 사용
   - 중요 이벤트만 알림

---

## 🔧 개발자 정보

### Kotlin/Java API 사용

#### 1. 기본 API 접근

```kotlin
// Territory Plugin 인스턴스 가져오기
val territoryPlugin = Bukkit.getPluginManager().getPlugin("territory_Plugin") as Territory_Plugin

// 또는 의존성 주입
class MyPlugin : JavaPlugin() {
    private lateinit var territoryAPI: Territory_Plugin
    
    override fun onEnable() {
        territoryAPI = server.pluginManager.getPlugin("territory_Plugin") as Territory_Plugin
    }
}
```

#### 2. 영토 확인 API

```kotlin
// 청크 소유자 확인
val chunkKey = "${world.name};${chunk.x};${chunk.z}"
val owner = territoryPlugin.databaseManager.getChunkOwner(chunkKey)

if (owner != null) {
    player.sendMessage("이 땅은 $owner 국가 소유입니다!")
} else {
    player.sendMessage("주인 없는 땅입니다!")
}

// 플레이어가 서 있는 위치의 영토
fun getPlayerTerritory(player: Player): String? {
    val chunk = player.location.chunk
    val chunkKey = "${player.world.name};${chunk.x};${chunk.z}"
    return territoryPlugin.databaseManager.getChunkOwner(chunkKey)
}

// 특정 위치가 특정 국가의 땅인지 확인
fun isNationTerritory(location: Location, nationName: String): Boolean {
    val chunk = location.chunk
    val chunkKey = "${location.world.name};${chunk.x};${chunk.z}"
    val owner = territoryPlugin.databaseManager.getChunkOwner(chunkKey)
    return owner == nationName
}
```

#### 3. 점령석 관리 API

```kotlin
// 점령석 설치
val stone = territoryPlugin.territoryManager.placeStone(location, ownerGroup)
if (stone != null) {
    player.sendMessage("점령석 설치 성공!")
} else {
    player.sendMessage("점령석 설치 실패!")
}

// 점령석 정보 조회
val stone = territoryPlugin.databaseManager.getStoneByLocation(location)
if (stone != null) {
    println("소유자: ${stone.ownerGroup}")
    println("티어: ${stone.currentTier}")
    println("점령 시간: ${stone.getOccupationTime()}초")
}

// 특정 국가의 모든 점령석
val stones = territoryPlugin.databaseManager.getStonesByTeam(nationName)
stones.forEach { stone ->
    println("점령석: ${stone.location}, 티어: ${stone.currentTier}")
}

// 점령석 업그레이드
val success = territoryPlugin.territoryManager.upgradeStone(stone)
if (success) {
    player.sendMessage("업그레이드 성공!")
}

// 점령석 파괴 및 영토 이전
territoryPlugin.territoryManager.destroyStone(stone, newOwnerGroup)
```

#### 4. 전쟁 시스템 API

```kotlin
// 전쟁 상태 확인
val isAtWar = territoryPlugin.warManager.isInGlobalWar(nationName)
if (isAtWar) {
    player.sendMessage("$nationName 은(는) 현재 전쟁 중입니다!")
}

// 전쟁 선포
territoryPlugin.warManager.declareGlobalWar(nationName)

// 전쟁 종료
territoryPlugin.warManager.endGlobalWar(nationName)

// 교전 가능 여부 확인
val canFight = territoryPlugin.warManager.canEngage(attackerNation, defenderNation)
if (!canFight) {
    event.isCancelled = true
    player.sendMessage("전쟁 중이 아니면 공격할 수 없습니다!")
}

// 전쟁 준비 시간 확인
val timeLeft = territoryPlugin.warManager.getTimeUntilWar(nationName)
if (timeLeft > 0) {
    player.sendMessage("전쟁까지 ${timeLeft}초 남았습니다!")
}
```

#### 5. 통계 시스템 API

```kotlin
// 국가 통계 조회
val stats = territoryPlugin.statsManager.getNationStats(nationName)
if (stats != null) {
    player.sendMessage("=== $nationName 통계 ===")
    player.sendMessage("영토: ${stats.totalChunks} 청크")
    player.sendMessage("점령석: ${stats.totalStones}개")
    player.sendMessage("최고 티어: ${stats.highestTier}")
    player.sendMessage("영토 점수: ${stats.getTerritoryScore()}")
}

// 국가 랭킹
val ranking = territoryPlugin.statsManager.getNationRanking(nationName)
player.sendMessage("$nationName 순위: $ranking")

// 전체 국가 통계 (랭킹순)
val allStats = territoryPlugin.statsManager.getAllNationStats()
allStats.forEachIndexed { index, stats ->
    println("${index + 1}. ${stats.displayName} - ${stats.getTerritoryScore()}")
}

// 가장 가까운 적 점령석 찾기
val nearestStone = territoryPlugin.statsManager.findNearestEnemyStone(
    player.location, 
    playerNation
)
if (nearestStone != null) {
    player.compassTarget = nearestStone
    player.sendMessage("나침반이 가장 가까운 적 점령석을 가리킵니다!")
}
```

#### 6. 플레이어 국가 확인

```kotlin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache

// 플레이어의 국가 확인 (캐싱됨 - 5분)
val playerNation = PlayerGroupCache.getPlayerGroup(player)
player.sendMessage("당신의 국가: $playerNation")

// 캐시 무효화 (그룹 변경 시)
PlayerGroupCache.invalidate(player.uniqueId)
```

#### 7. 커스텀 이벤트 리스너

```kotlin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerMoveEvent

class TerritoryListener(private val territoryAPI: Territory_Plugin) : Listener {
    
    // 점령석 파괴 감지
    @EventHandler
    fun onStoneBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type == Material.OBSIDIAN) {
            val stone = territoryAPI.databaseManager.getStoneByLocation(block.location)
            if (stone != null) {
                // 점령석입니다!
                val player = event.player
                val playerNation = PlayerGroupCache.getPlayerGroup(player)
                
                if (playerNation != stone.ownerGroup) {
                    // 적이 점령석을 파괴하려 함
                    Bukkit.broadcast("§c경고! ${stone.ownerGroup}의 점령석이 공격받고 있습니다!")
                }
            }
        }
    }
    
    // 영토 진입 감지
    private val lastTerritory = mutableMapOf<UUID, String?>()
    
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to ?: return
        
        // 청크 변경 확인
        if (from.chunk != to.chunk) {
            val player = event.player
            val chunkKey = "${to.world.name};${to.chunk.x};${to.chunk.z}"
            val owner = territoryAPI.databaseManager.getChunkOwner(chunkKey)
            
            // 이전 영토와 다른지 확인
            if (owner != lastTerritory[player.uniqueId]) {
                lastTerritory[player.uniqueId] = owner
                
                if (owner != null) {
                    player.sendTitle("§6$owner", "§e국가 영토", 10, 40, 10)
                } else {
                    player.sendTitle("§7무주지", "§f주인 없는 땅", 10, 40, 10)
                }
            }
        }
    }
}
```

#### 8. 영토 보호 플러그인 통합

```kotlin
class TerritoryProtectionPlugin : JavaPlugin() {
    private lateinit var territoryAPI: Territory_Plugin
    
    override fun onEnable() {
        territoryAPI = server.pluginManager.getPlugin("territory_Plugin") as Territory_Plugin
        server.pluginManager.registerEvents(ProtectionListener(), this)
    }
    
    inner class ProtectionListener : Listener {
        
        @EventHandler(priority = EventPriority.HIGH)
        fun onBlockBreak(event: BlockBreakEvent) {
            if (event.isCancelled) return
            
            val player = event.player
            val location = event.block.location
            
            if (!canPlayerBuild(player, location)) {
                event.isCancelled = true
                player.sendMessage("§c이 영토에서는 블록을 부술 수 없습니다!")
            }
        }
        
        @EventHandler(priority = EventPriority.HIGH)
        fun onBlockPlace(event: BlockPlaceEvent) {
            if (event.isCancelled) return
            
            val player = event.player
            val location = event.block.location
            
            if (!canPlayerBuild(player, location)) {
                event.isCancelled = true
                player.sendMessage("§c이 영토에서는 블록을 설치할 수 없습니다!")
            }
        }
        
        private fun canPlayerBuild(player: Player, location: Location): Boolean {
            // 관리자는 항상 가능
            if (player.hasPermission("territory.admin")) return true
            
            val chunk = location.chunk
            val chunkKey = "${location.world.name};${chunk.x};${chunk.z}"
            val owner = territoryAPI.databaseManager.getChunkOwner(chunkKey)
            
            // 주인 없는 땅은 가능
            if (owner == null) return true
            
            val playerNation = PlayerGroupCache.getPlayerGroup(player)
            
            // 본인 땅이면 가능
            if (owner == playerNation) return true
            
            // 전쟁 중이면 가능
            if (territoryAPI.warManager.isInGlobalWar(owner) || 
                territoryAPI.warManager.isInGlobalWar(playerNation)) {
                return true
            }
            
            // 그 외는 불가
            return false
        }
    }
}
```

#### 9. 경제 연동 예제

```kotlin
import net.milkbowl.vault.economy.Economy

class TerritoryEconomyIntegration(
    private val territoryAPI: Territory_Plugin,
    private val economy: Economy
) {
    
    // 영토 세금 징수
    fun collectTerritoryTax(nationName: String): Double {
        val stats = territoryAPI.statsManager.getNationStats(nationName) ?: return 0.0
        val chunks = stats.totalChunks
        val taxPerChunk = 10.0
        val totalTax = chunks * taxPerChunk
        
        // 온라인 플레이어들에게서 세금 징수
        Bukkit.getOnlinePlayers()
            .filter { PlayerGroupCache.getPlayerGroup(it) == nationName }
            .forEach { player ->
                if (economy.has(player, totalTax)) {
                    economy.withdrawPlayer(player, totalTax)
                    player.sendMessage("§e영토 세금 -$${totalTax} §7(청크: $chunks)")
                } else {
                    player.sendMessage("§c세금을 낼 돈이 부족합니다!")
                }
            }
        
        return totalTax
    }
    
    // 점령석 업그레이드 비용 확인
    fun canAffordUpgrade(player: Player, stone: OccupationStone): Boolean {
        val currentTier = stone.currentTier.ordinal + 1
        val nextTier = currentTier + 1
        val cost = territoryAPI.configManager.getUpgradeMoney(currentTier, nextTier)
        
        return economy.has(player, cost)
    }
}
```

#### 10. Discord 연동 예제

```kotlin
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

class TerritoryDiscordIntegration(
    private val territoryAPI: Territory_Plugin,
    private val jda: JDA,
    private val channelId: String
) {
    
    // 전쟁 선포 알림
    fun notifyWarDeclaration(nationName: String) {
        val channel = jda.getTextChannelById(channelId) ?: return
        
        val embed = EmbedBuilder()
            .setTitle("⚔️ 전쟁 선포!")
            .setDescription("**$nationName**이(가) 전면전을 선포했습니다!")
            .setColor(Color.RED)
            .addField("전쟁 시작", "10분 후", false)
            .addField("상태", "준비 중...", false)
            .setTimestamp(java.time.Instant.now())
            .build()
        
        channel.sendMessageEmbeds(embed).queue()
    }
    
    // 점령석 파괴 알림
    fun notifyStoneDestruction(stone: OccupationStone, destroyer: String) {
        val channel = jda.getTextChannelById(channelId) ?: return
        
        val location = stone.location
        val embed = EmbedBuilder()
            .setTitle("💥 점령석 파괴!")
            .setColor(Color.ORANGE)
            .addField("피해국", stone.ownerGroup, true)
            .addField("공격자", destroyer, true)
            .addField("위치", "${location.world.name} (${location.blockX}, ${location.blockZ})", false)
            .addField("티어", stone.currentTier.tierName, true)
            .setTimestamp(java.time.Instant.now())
            .build()
        
        channel.sendMessageEmbeds(embed).queue()
    }
    
    // 국가 랭킹 전송
    fun sendRanking() {
        val channel = jda.getTextChannelById(channelId) ?: return
        val allStats = territoryAPI.statsManager.getAllNationStats()
        
        val embed = EmbedBuilder()
            .setTitle("🏆 국가 랭킹")
            .setColor(Color.GOLD)
        
        allStats.take(10).forEachIndexed { index, stats ->
            val medal = when(index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${index + 1}."
            }
            
            embed.addField(
                "$medal ${stats.displayName}",
                "점수: ${stats.getTerritoryScore()} | 청크: ${stats.totalChunks} | 점령석: ${stats.totalStones}",
                false
            )
        }
        
        embed.setTimestamp(java.time.Instant.now())
        channel.sendMessageEmbeds(embed.build()).queue()
    }
}
```

---

### 📦 Maven/Gradle 의존성

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(fileTree("libs") { include("territory_Plugin.jar") })
}
```

#### Maven

```xml
<dependency>
    <groupId>kr.skarch</groupId>
    <artifactId>territory_Plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

---

### 🎯 API 사용 체크리스트

- [ ] plugin.yml에 `depend: [territory_Plugin]` 추가
- [ ] Territory_Plugin 인스턴스 가져오기
- [ ] null 체크 (플러그인 비활성화 대비)
- [ ] 비동기 처리 (DB 조회가 많은 경우)
- [ ] 캐싱 활용 (PlayerGroupCache 등)
- [ ] 에러 처리 (try-catch)

---

---

## 🐛 문제 해결

### 일반적인 문제

#### 1. 점령석이 설치되지 않아요
```
원인: 해당 위치에 이미 블록이 있음
해결: 빈 공간에 설치하세요
```

#### 2. BlueMap에 마커가 안 보여요
```
원인: BlueMap 플러그인 미설치
해결:
1. BlueMap 설치
2. 서버 재시작
3. 로그 확인: [Territory] BlueMap API connected!
```

#### 3. 업그레이드가 안 돼요
```
원인: 요구사항 미충족
확인:
- 점령 시간 충족?
- 돈 충분?
- 소유권 확인?
```

#### 4. 전쟁이 종료가 안 돼요
```
해결: /territory endwar <국가명>
```

---

## 📈 성능 최적화

### 권장 설정

```yaml
# config.yml
occupation-stone:
  # Y좌표 적절히 설정 (너무 높거나 낮으면 문제)
  spawn-y-coordinate: 70
  
war:
  # 전쟁 준비 시간 (너무 짧으면 부담)
  preparation-time: 600  # 10분
```

### 서버 성능

- ✅ 비동기 BlueMap 업데이트
- ✅ LuckPerms 캐싱 (5분)
- ✅ 데이터베이스 인덱스 최적화
- ✅ 자동 연결 복구

---

## 📝 변경 로그

### v1.0-SNAPSHOT (2025-12-11) - 최신
#### 🐛 버그 수정
- 🔧 **점령석 설치 문제 해결**: 60+ 종류의 자연 블록에서 설치 가능하도록 개선
- 🔧 **콘솔 명령어 지원**: reload, startwar, endwar 명령어 콘솔 사용 가능
- 🔧 **팀 인식 문제 해결**: team.yml에 없는 그룹은 "팀없음"으로 처리

#### ✨ 개선사항
- ⚡ **PlaceholderAPI 최적화**: PlayerGroupCache 사용으로 성능 향상
- 📝 **명령어 추가**: score, scorenow, cancel 명령어 추가
- 🎯 **에러 처리 개선**: 더 안정적인 팀 감지 시스템

#### 📋 기존 기능
- ✅ 점령석 시스템 (5단계 티어)
- ✅ 전쟁 시스템 (전면전)
- ✅ BlueMap 완전 연동
- ✅ 다국어 지원 (lang.yml)
- ✅ 커스텀 모델 데이터 (items.yml)
- ✅ 통계 및 랭킹 시스템
- ✅ 전쟁 이력 로깅
- ✅ 실시간 알림 시스템
- ✅ 성능 최적화 (캐싱)

---

## 🤝 기여

버그 리포트 및 기능 제안은 환영합니다!

### 버그 리포트
```
1. 버그 상세 설명
2. 재현 방법
3. 예상 결과 vs 실제 결과
4. 서버 로그
```

### 기능 제안
```
1. 기능 설명
2. 사용 사례
3. 예상 효과
```

---

## 📄 라이센스

이 플러그인은 MIT 라이센스 하에 배포됩니다.

---

## 📞 지원

### 문서
- 📖 이 README
- 🔧 config.yml 주석
- 💬 명령어 도움말 (`/territory`)

### 커뮤니티
- Discord: dev.skarch
- GitHub: [SKARCH218](https://github.com/SKARCH218)

---

## 🎉 특별 감사

- **Paper**: 강력한 서버 플랫폼
- **LuckPerms**: 권한 시스템
- **BlueMap**: 웹 맵 플러그인
- **Vault**: 경제 시스템
- **PlaceholderAPI**: 플레이스홀더 시스템

---

<div align="center">

**Territory Plugin** - 당신의 서버에 전쟁과 영토를!


[⬆ 맨 위로](#-territory-plugin---완벽-가이드)

</div>

