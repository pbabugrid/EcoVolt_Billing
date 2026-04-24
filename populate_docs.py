import json
files = {
    'docs/TECHSTACK.md': """# Tech Stack
- **Language:** Java 17
- **Build Tool:** Maven
- **Database:** MySQL 8+
- **Infrastructure:** Docker & Docker Compose
- **Testing:** JUnit 5, Mockito
""",
    'docs/DEPENDENCIES.md': """# Dependencies
### Production
- `mysql-connector-j` (8.3.0) - MySQL Database Driver
### Development/Test
- `junit-jupiter` (5.10.2) - Testing Framework
- `mockito-junit-jupiter` (5.11.0) - Mocking Framework
### Plugins
- `maven-jar-plugin` (3.3.0)
- `maven-surefire-plugin` (3.2.5)
""",
    'docs/ARCHITECTURE.md': """# Architecture Overview
EcoVolt_Billing is a Java batch processing application.
## Components:
- **App (`com.ecovolt.app`)**: Entry points (`MainApplication`) and processing logic (`BillingProcessor`, `CsvReader`).
- **Config (`com.ecovolt.config`)**: Configuration logic like `DatabaseConfig`.
- **DAO (`com.ecovolt.dao`)**: Database Access Objects handling persistence (`BillingDAO`, `CustomerDAO`, etc.).
- **Model (`com.ecovolt.model`)**: Domain modelsfiles = {
r`    'doc`,- **Language:** Java 17
- **Build Tool:tr- **Build Tool:** Mavera- **Database:** MySQL on- **Infrastructure:** Dra- **Testing:** JUnit 5, Mockito
""",
    'dong""",
    'docs/DEPENDENCIES.mdct   :
### Production
- `mysql-connector-j` (8.3.0)om- `mysql-co""",### Development/Test
- `junit-jupiter` (5.10.2) - Te_B- `junit-jupiter` ( b- `mockito-junit-jupiter` (5.11.0) - Mocking te### Plugins
- `maven-jar-plugin` (3.3.0)
- `maven-surtr- `maven-jse- `maven-surefire-plugin` (nd""",
    'docs/ARCHITECTURE.md':My    dEcoVolt_Billing is a Java batch processing applicatioth## Components:
- **App (`com.ecovolt.app`)**: Entry poNS- **App (`comsu- **Config (`com.ecovolt.config`)**: Configuration logic like `DatabaseConfig`.
- **DAO (`com.ecovolt.dao`)**: Databaske- **DAO (`com.ecovolt.dao`)**: Database Access Objects handling persistence (`ea- **Model (`com.ecovolt.model`)**: Domain modelsfiles = {
r`    'doc`,- **Language:** Java 17
- **Build Tool:tretr`    'doc`,- **Language:** Java 17
- **Build Tool:tr- *ck- **Build Tool:tr- **Build Tool:**da""",
    'dong""",
    'docs/DEPENDENCIES.mdct   :
### Production
- `mysql-connector-j` (8.3.0)om- `mysql-co""",### Deve-i   on    'docs/DEe ### Production
- `mysql-connecly- `mysql-connas- `junit-jupiter` (5.10.2) - Te_B- `junit-jupiter` ( b- `mockito-en- `maven-jar-plugin` (3.3.0)
- `maven-surtr- `maven-jse- `maven-surefire-plugin` (nd""",
    'docs/ARCHITECTUREh,- `maven-surtr- `maven-jse-(c    'docs/ARCHITECTURE.md':My    dEcoVolt_Billing is a Jaecho '# Tech Stack\n\n- **Language:** Java 17\n- **Build Tool:** Maven\n- **Database:** MySQL 8+\n- **Infrastructure:** Docker & Docker Compose\n- **Testing:** JUnit 5, Mockito' > docs/TECHSTACK.md
echo '# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mockito-junit-jupiter` (5.11.0) - Mocking Framework' > docs/DEPENDENCIES.md
echo '# Architecture Overview\n\nEcoVolt_Billing is a Java batch processing application.\n\n## Layers:\n- **App (`com.ecovolt.app`)**: Entry points and core batch processing.\n- **Config (`com.ecovolt.config`)**: App configuration.\n- **DAO (`com.ecovolt.dao`)**: Database Access Objects.\n- **Model (`com.ecovolt.model`)**: Domain models and DB Entities.\n- **Strategy (`com.ecovolt.strategy`)**: Pricing strategy implementations.' > docs/ARCHITECTURE.md
echo '# Project Context\n\nEcoVolt_Billing is a utility billing application that reads customer meter readings from CSV fileecho '# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mockito-junit-jupiter` (5.# echo '# Architecture Overview\n\nEcoVolt_Billing is a Java batch processing application.\n\n## Layers:\n- **App (`com.ecovolt.app`)**: Entry points and core batch processing.\n- **Config (`com.ecovolt.config`)**: App configuration.\n- **DAO (`cs/echo '# Project Context\n\nEcoVolt_Billing is a utility billing application that reads customer meter readings from CSV fileecho '# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mockito-junit-jupiter` (5.# echo '# Architecture Overview\n\nEcoVolt_Billing is a Java batch processicat docs/TECHSTACK.md
cat docs/DEPENDENCIES.md
cat > write_files.py << 'EOF_PYTHON'
import os
with open('docs/TECHSTACK.md', 'w') as f: f.write("# Tech Stack\n\n- **Language:** Java 17\n- **Build Tool:** Maven\n- **Database:** MySQL 8+\n- **Infrastructure:** Docker & Docker Compose\n- **Testing:** JUnit 5, Mockito")
with open('docs/DEPENDENCIES.md', 'w') as f: f.write("# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mockito-junit-jupiter` (5.11.0) - Mocking Framework")
with open('docs/ARCHITECTURE.md', 'w') as f: f.write("# Architecture Overview\n\nEcoVolt_Billing is a Java batch processing application.\n\n## Layers:\n- **App (`com.ecovolt.app`)**: Entry points and core batch processing.\n- **Config (`com.ecovolt.config`)**: App configuration.\n- **DAO (`com.ecovolt.dao`)**: Database Access Objects.\n- **Model (`com.ecovolt.model`)**: Domain models and DB Entities.\n- **Strategy (`com.ecovolt.strategy`)**: Pricing strategy implementations.")import os
with open('docs/TECHSTACKaswith opeitwith open('docs/DEPENDENCIES.md', 'w') as f: f.write("# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mowithwith open('docs/ARCHITECTURE.md', 'w') as f: f.write("# Architecture Overview\n\nEcoVolt_Billing is a Java batch processing application.\n\n## Layers:\n- **App (`com.ecovolt.app`)**: Entry points and core batch processing.\n- **Config (`com.ecovolt.config`)**: App confia with open('docs/TECHSTACKaswith opeitwith open('docs/DEPENDENCIES.md', 'w') as f: f.write("# Dependencies\n\n### Production\n- `mysql-connector-j` (8.3.0) - MySQL Database Driver\n\n### Development/Test\n- `junit-jupiter` (5.10.2) - Testing Framework\n- `mowithwith open('docs/ARCHITECTURE.md', 'w') as f: f.write("# Architecture Overview\n\nEcoVolt_Billing is a Java batch processing application.\n\n## Layers:\n- **App (`com.ecovolt.app`)**: Entry points and core batch processing.\n- **e("# Agent Memory\n\nAgent workspace initialized. Context loaded for EcoVolt_Billing.")
EOF_PYTHON
python3 write_files.py
cat docs/DEPENDENCIES.md
