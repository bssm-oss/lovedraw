# Operator Chat Seed - Lovedraw Demo

Date: 2026-06-10

## Chat Transcript

**Operator:** 오늘 시연에서 Lovedraw가 실제 팀처럼 움직이는 화면이 필요해. 게시판 만들기 목표로 시작해줘.

**CEO:** 목표를 접수했습니다. Product Strategist에게 MVP 범위를 정리시키고 Engineering Lead에게 구현 흐름을 넘기겠습니다.

**Product Strategist:** 게시판 MVP는 공지/게시글 목록, 작성 진입점, 최근 운영 기록, 홍보 상태를 한 화면에서 확인하는 구조가 좋습니다.

**Engineering Lead:** UI Builder는 첫 화면 구성을 잡고, Builder는 이후 실제 게시판 구현 이슈를 받을 준비를 해주세요.

**Marketing Operator:** Threads와 Product Hunt 문구를 준비했습니다. 실제 발행은 정책과 계정 확인 후 진행하겠습니다.

**QA:** 시연 전에는 목표 삭제, 이슈 삭제, 드롭다운 애니메이션, 운영 채팅, 미팅룸 애니메이션, 보고서 표시를 확인해야 합니다.

## Quick Commands Used

```bash
cotor company create --name "Lovedraw Demo" --root /Users/Projects/bssm-oss/lovedraw
cotor company goal create --title "게시판 만들기"
cotor company message send --from-agent-name CEO --to-agent-name "Product Strategist"
```

## Demo Talking Points

- Cotor can run a company-style local agent team around one repository.
- Agent messages are not static UI copy; they are stored in the company state.
- Reports and performance notes give the demonstration a realistic operating history.
