# 릴리스 가이드 (Maven Central)

이 프로젝트는 [Sonatype Central Portal](https://central.sonatype.com)을 통해 Maven Central에 배포합니다.
그룹 ID는 `io.github.ktae23` 입니다.

배포 자동화는 [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/) 플러그인이 담당하며,
GitHub Release가 발행되면 `.github/workflows/publish.yml`이 실행됩니다.

---

## 1. 최초 1회 준비 (사용자 직접 수행)

이 단계들은 계정·키·소유권 증명이 필요해 **저장소 소유자(ktae23)가 직접** 해야 합니다.

### 1-1. Central Portal 계정 + 네임스페이스 검증
1. https://central.sonatype.com 에 GitHub 계정으로 로그인.
2. **Namespaces → Add Namespace** 에서 `io.github.ktae23` 추가.
3. GitHub 계정 기반 네임스페이스는 안내되는 **검증용 임시 리포지토리**를 GitHub에 만들면 자동 검증됩니다.
   (`io.github.<GitHub아이디>` 형태는 GitHub 소유권으로 증명)

### 1-2. Publisher 토큰 발급
1. Central Portal → **Account → Generate User Token**.
2. 출력된 `username` / `password` 토큰 쌍을 안전히 보관.

### 1-3. GPG 서명 키 생성
Maven Central은 모든 아티팩트에 GPG 서명을 요구합니다.

```bash
# 키 생성 (이메일/이름 입력, 비밀번호 설정)
gpg --gen-key

# 키 ID 확인
gpg --list-secret-keys --keyid-format=long

# 공개키를 키서버에 배포 (KEYID는 위에서 확인한 long 형식)
gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>

# in-memory 서명용 개인키 export (ASCII armored)
gpg --export-secret-keys --armor <KEYID>
```

### 1-4. GitHub Actions 시크릿 등록
저장소 **Settings → Secrets and variables → Actions**에 다음 4개 등록:

| 시크릿 이름 | 값 |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | 1-2의 토큰 username |
| `MAVEN_CENTRAL_PASSWORD` | 1-2의 토큰 password |
| `SIGNING_KEY` | 1-3에서 export한 ASCII armored 개인키 전체(`-----BEGIN...`부터 `...END-----`까지) |
| `SIGNING_KEY_PASSWORD` | GPG 키 비밀번호 |

---

## 2. 로컬에서 배포/테스트

`~/.gradle/gradle.properties`에 자격증명을 넣으면 로컬에서도 배포 가능합니다(커밋 금지!):

```properties
mavenCentralUsername=<토큰 username>
mavenCentralPassword=<토큰 password>
signingInMemoryKey=<ASCII armored 개인키(\n을 실제 개행으로)>
signingInMemoryKeyPassword=<GPG 비밀번호>
```

```bash
# 로컬 Maven 저장소로 시험 배포 (자격증명 불필요)
./gradlew publishToMavenLocal

# Central Portal 스테이징으로 업로드 (수동 릴리스 대기)
./gradlew publishToMavenCentral
```

현재 설정은 `automaticRelease = false` 이므로, 업로드 후 Central Portal의
**Deployments** 화면에서 내용 확인 후 **Publish** 버튼으로 최종 공개합니다.
자동 공개를 원하면 `lib/build.gradle.kts`·`spring-boot-starter/build.gradle.kts`의
`publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)`로 변경하세요.

---

## 3. 정식 릴리스 절차 (SemVer)

1. `build.gradle.kts`의 `version`을 SemVer로 올린다 (예: `1.1.0` → `1.2.0`).
   - MAJOR: 호환 깨짐 / MINOR: 기능 추가(하위호환) / PATCH: 버그 수정
2. `CHANGELOG.md`에 변경 내역을 추가한다.
3. 변경을 `main`에 머지한다.
4. GitHub에서 **Releases → Draft a new release** →
   태그를 `v<버전>` (예: `v1.2.0`)로 생성하고 발행한다.
5. `publish.yml`이 자동 실행되어 Central Portal로 업로드된다.
6. (`automaticRelease=false`인 경우) Central Portal에서 **Publish**로 최종 공개.

> 태그는 반드시 SemVer(`vMAJOR.MINOR.PATCH`)를 따른다. 과거의 임의 태그(`latest`, `realese` 등)는 사용하지 않는다.
