# 광역 세뇌 스킬 - Skript 사용 예시

## Territory API 사용법

Territory Plugin v1.3+에서는 외부 스크립트를 위한 강력한 API를 제공합니다.

### API 접근 방법

```skript
# Territory Plugin 인스턴스 가져오기
set {_plugin} to plugin "territory_Plugin"
set {_api} to {_plugin}.territoryAPI
```

---

## 광역 세뇌 스킬 구현 예시

### 기본 버전

```skript
command /광역세뇌 [<text>]:
    permission: skill.mindcontrol
    trigger:
        # API 가져오기
        set {_plugin} to plugin "territory_Plugin"
        set {_api} to {_plugin}.territoryAPI
        
        if arg-1 is not set:
            # 현재 위치의 영토 이름 확인
            set {_regionName} to {_api}.getRegionNameAt(player)
            
            if {_regionName} is not set:
                send "§c현재 위치에 점령석이 없습니다!" to player
                stop
            
            send "§e현재 지역: §f%{_regionName}%" to player
            send "§e사용법: /광역세뇌 <지역명>" to player
            stop
        
        # 플레이어 그룹 가져오기
        set {_playerGroup} to {_api}.getTerritoryOwnerAt(player)
        
        if {_playerGroup} is "팀없음":
            send "§c팀에 속해있지 않습니다!" to player
            stop
        
        # 영토 탈취
        set {_success} to {_api}.transferTerritoryOwnership(arg-1, {_playerGroup})
        
        if {_success} is true:
            send "§a§l광역 세뇌 성공!" to player
            send "§e%arg-1% 지역을 탈취했습니다!" to player
            broadcast "§c⚠ 광역 세뇌! §e%arg-1%§c 지역이 탈취되었습니다!"
        else:
            send "§c해당 지역을 찾을 수 없습니다!" to player
```

### 고급 버전 (쿨다운, 비용, 조건)

```skript
command /광역세뇌 [<text>]:
    permission: skill.mindcontrol
    cooldown: 1 hour
    cooldown message: §c이 스킬은 %remaining time%후에 사용할 수 있습니다!
    trigger:
        # API 가져오기
        set {_plugin} to plugin "territory_Plugin"
        set {_api} to {_plugin}.territoryAPI
        
        # 비용 확인 (Vault 필요)
        if player's balance < 100000:
            send "§c비용이 부족합니다! (필요: $100,000)" to player
            cancel the cooldown
            stop
        
        if arg-1 is not set:
            # 현재 위치의 영토 정보
            set {_currentRegion} to {_api}.getRegionNameAt(player)
            set {_currentOwner} to {_api}.getTerritoryOwnerAt(player)
            
            if {_currentRegion} is not set:
                send "§c현재 위치에 점령석이 없습니다!" to player
                cancel the cooldown
                stop
            
            send "§6=== 광역 세뇌 스킬 ===" to player
            send "§e현재 지역: §f%{_currentRegion}%" to player
            send "§e소유자: §f%{_currentOwner}%" to player
            send "" to player
            send "§e사용법: /광역세뇌 <지역명>" to player
            send "§e비용: §6$100,000" to player
            send "§e쿨다운: §61시간" to player
            cancel the cooldown
            stop
        
        # 플레이어 팀 확인
        set {_luckperms} to plugin "LuckPerms".getApi()
        set {_user} to {_luckperms}.getUserManager().getUser(player's uuid)
        set {_playerGroup} to {_user}.getPrimaryGroup()
        
        if {_playerGroup} is "default":
            send "§c팀에 속해있지 않습니다!" to player
            cancel the cooldown
            stop
        
        # 대상 지역 확인
        set {_targetOwner} to {_api}.getRegionOwner(arg-1)
        
        if {_targetOwner} is not set:
            send "§c해당 지역을 찾을 수 없습니다!" to player
            cancel the cooldown
            stop
        
        if {_targetOwner} is {_playerGroup}:
            send "§c이미 우리 팀의 영토입니다!" to player
            cancel the cooldown
            stop
        
        # 확인 GUI
        open chest with 3 rows named "§c광역 세뇌 - %arg-1%" to player
        
        # 확인 버튼 (녹색 유리)
        format slot 11 of player with lime stained glass pane named "§a§l확인" with lore "§7클릭하여 광역 세뇌 실행||§e비용: §6$100,000||§e대상: §f%arg-1%||§e현재 소유자: §c%{_targetOwner}%" to run:
            close player's inventory
            
            # 비용 차감
            remove 100000 from player's balance
            
            # 영토 탈취
            set {_success} to {_api}.transferTerritoryOwnership(arg-1, {_playerGroup})
            
            if {_success} is true:
                send "§a§l광역 세뇌 성공!" to player
                send "§e%arg-1% 지역을 탈취했습니다!" to player
                send "§e비용: §6-$100,000" to player
                
                # 이펙트
                play sound "entity.wither.spawn" at player's location with volume 1 and pitch 2
                
                # 브로드캐스트는 API에서 자동으로 수행됨
            else:
                send "§c광역 세뇌에 실패했습니다!" to player
                add 100000 to player's balance
        
        # 취소 버튼 (빨간 유리)
        format slot 15 of player with red stained glass pane named "§c§l취소" to run:
            close player's inventory
            send "§e광역 세뇌를 취소했습니다." to player
            cancel the cooldown
```

---

## Territory API 전체 메서드

### 1. 영토 이름 조회

```skript
# 특정 위치의 영토 이름 (지역명)
set {_regionName} to {_api}.getRegionNameAt(player's location)

# 플레이어 위치의 영토 이름
set {_regionName} to {_api}.getRegionNameAt(player)
```

### 2. 영토 소유자 조회

```skript
# 특정 위치의 소유자 그룹
set {_owner} to {_api}.getTerritoryOwnerAt(player's location)

# 플레이어 위치의 소유자
set {_owner} to {_api}.getTerritoryOwnerAt(player)

# 특정 지역의 소유자
set {_owner} to {_api}.getRegionOwner("서울")
```

### 3. 영토 소유권 변경 ⭐

```skript
# 영토 탈취 (광역 세뇌)
set {_success} to {_api}.transferTerritoryOwnership("서울", "korea")

# 반환값: true = 성공, false = 실패
# 자동으로 BlueMap 업데이트 및 브로드캐스트 수행
```

### 4. 영토 정보 조회

```skript
# 특정 지역의 청크 수
set {_chunkCount} to {_api}.getRegionChunkCount("서울")

# 모든 지역명 목록
set {_allRegions::*} to {_api}.getAllRegionNames()

# 특정 그룹이 소유한 지역들
set {_koreaRegions::*} to {_api}.getRegionsByOwner("korea")

# 지역 존재 여부
if {_api}.doesRegionExist("서울") is true:
    send "서울 지역이 존재합니다!"
```

### 5. 위치 기반 확인

```skript
# 특정 위치가 특정 지역에 속하는지
if {_api}.isLocationInRegion(player's location, "서울") is true:
    send "당신은 서울에 있습니다!"
```

### 6. 영지의 청크 위치 조회 ⭐ NEW!

```skript
# 특정 영지의 모든 청크 위치 반환
set {_chunks::*} to {_api}.getRegionChunkLocations("서울")

# 청크 위치 파싱
loop {_chunks::*}:
    # loop-value = "world;10;20" 형식
    set {_parts::*} to loop-value split at ";"
    set {_world} to {_parts::1}
    set {_chunkX} to {_parts::2} parsed as integer
    set {_chunkZ} to {_parts::3} parsed as integer
    
    send "청크: %{_world}% (%{_chunkX}%, %{_chunkZ}%)" to player

# 반환값: ["world;10;20", "world;10;21", ...] 형식
# 영지가 없으면 빈 리스트 반환
```

---

## 추가 스킬 예시

### 영토 정찰 스킬

```skript
command /정찰:
    cooldown: 30 seconds
    trigger:
        set {_plugin} to plugin "territory_Plugin"
        set {_api} to {_plugin}.territoryAPI
        
        set {_regionName} to {_api}.getRegionNameAt(player)
        set {_owner} to {_api}.getTerritoryOwnerAt(player)
        set {_chunkCount} to {_api}.getRegionChunkCount({_regionName})
        
        if {_regionName} is not set:
            send "§c이곳은 무주지입니다!" to player
            stop
        
        send "§6=== 영토 정찰 ===" to player
        send "§e지역명: §f%{_regionName}%" to player
        send "§e소유자: §f%{_owner}%" to player
        send "§e크기: §f%{_chunkCount}% 청크" to player
```

### 영토 목록 확인

```skript
command /영토목록 [<text>]:
    trigger:
        set {_plugin} to plugin "territory_Plugin"
        set {_api} to {_plugin}.territoryAPI
        
        if arg-1 is not set:
            # 모든 지역 목록
            set {_regions::*} to {_api}.getAllRegionNames()
            
            send "§6=== 전체 영토 목록 ===" to player
            loop {_regions::*}:
                set {_owner} to {_api}.getRegionOwner(loop-value)
                send "§e- §f%loop-value% §7(소유: %{_owner}%)" to player
        else:
            # 특정 팀의 영토 목록
            set {_regions::*} to {_api}.getRegionsByOwner(arg-1)
            
            send "§6=== %arg-1% 팀의 영토 ===" to player
            loop {_regions::*}:
                set {_chunkCount} to {_api}.getRegionChunkCount(loop-value)
                send "§e- §f%loop-value% §7(%{_chunkCount}% 청크)" to player
```

### 영토 청크 위치 확인 (NEW!)

```skript
command /영토위치 <text>:
    trigger:
        set {_plugin} to plugin "territory_Plugin"
        set {_api} to {_plugin}.territoryAPI
        
        # 영지의 모든 청크 위치 조회
        set {_chunks::*} to {_api}.getRegionChunkLocations(arg-1)
        
        if size of {_chunks::*} is 0:
            send "§c'%arg-1%' 영토를 찾을 수 없습니다!" to player
            stop
        
        send "§6=== %arg-1% 영토 청크 위치 ===" to player
        send "§e총 %size of {_chunks::*}%개 청크" to player
        send "" to player
        
        loop {_chunks::*}:
            # "world;10;20" 형식 파싱
            set {_parts::*} to loop-value split at ";"
            set {_world} to {_parts::1}
            set {_x} to {_parts::2} parsed as integer
            set {_z} to {_parts::3} parsed as integer
            
            send "§7- §f%{_world}% §7청크 (§e%{_x}%§7, §e%{_z}%§7)" to player
```

---

## 주의사항

### ✅ DO
- API 사용 전 plugin이 null이 아닌지 확인
- transferTerritoryOwnership 반환값 확인
- 비용/쿨다운 시스템 구현 권장
- 확인 GUI로 실수 방지

### ❌ DON'T
- 같은 팀 영토를 탈취하지 않도록 확인
- 무주지(null) 탈취 시도 방지
- 너무 짧은 쿨다운 설정 지양
- 비용 없이 무제한 사용 금지

---

## 디버깅

```skript
command /apitest:
    permission: admin
    trigger:
        set {_plugin} to plugin "territory_Plugin"
        
        if {_plugin} is not set:
            send "§cTerritory Plugin not found!" to player
            stop
        
        set {_api} to {_plugin}.territoryAPI
        
        if {_api} is not set:
            send "§cTerritory API not initialized!" to player
            stop
        
        send "§aTerritory API OK!" to player
        
        # 현재 위치 정보
        set {_regionName} to {_api}.getRegionNameAt(player)
        set {_owner} to {_api}.getTerritoryOwnerAt(player)
        
        send "§e지역: %{_regionName}%" to player
        send "§e소유자: %{_owner}%" to player
```

---

## 완성된 광역 세뇌 스킬

위의 "고급 버전" 스크립트를 사용하면:
1. ✅ 1시간 쿨다운
2. ✅ $100,000 비용
3. ✅ 확인 GUI
4. ✅ 팀 확인
5. ✅ 대상 유효성 검사
6. ✅ BlueMap 자동 반영
7. ✅ 전체 브로드캐스트
8. ✅ 효과음

모든 기능이 포함되어 있습니다!

