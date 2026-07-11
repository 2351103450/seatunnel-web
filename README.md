<p align="center">
  <img
    src="https://github.com/user-attachments/assets/901d765c-cbd7-4f39-ae3a-de6716ae09f2"
    width="100%"
    alt="SeaTunnel Web Banner"
  />
</p>

<h1 align="center">SeaTunnel Web</h1>

<p align="center">
  A modern, visual, and production-oriented third-party Web UI for Apache SeaTunnel.
</p>

<p align="center">
  <a href="https://github.com/weifuwan/seatunnel-web/releases">
    <img src="https://img.shields.io/github/v/release/weifuwan/seatunnel-web?include_prereleases&style=flat-square" alt="Release" />
  </a>
  <a href="https://github.com/weifuwan/seatunnel-web/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/weifuwan/seatunnel-web?style=flat-square" alt="License" />
  </a>
  <a href="https://github.com/weifuwan/seatunnel-web/stargazers">
    <img src="https://img.shields.io/github/stars/weifuwan/seatunnel-web?style=flat-square" alt="GitHub Stars" />
  </a>
  <a href="https://github.com/weifuwan/seatunnel-web/issues">
    <img src="https://img.shields.io/github/issues/weifuwan/seatunnel-web?style=flat-square" alt="GitHub Issues" />
  </a>
  <img src="https://img.shields.io/badge/Java-21-blue?style=flat-square" alt="Java 21" />
  <img src="https://img.shields.io/badge/Node.js-%3E%3D20-blue?style=flat-square" alt="Node.js 20+" />
  <img src="https://img.shields.io/badge/SeaTunnel-2.3.13-blue?style=flat-square" alt="SeaTunnel 2.3.13" />
</p>

<p align="center">
  <a href="http://111.230.213.87:8000">Live Demo</a>
  ·
  <a href="https://doc.seatunnel-web.com/">Documentation</a>
  ·
  <a href="http://111.230.213.87:9001/">Home</a>
  ·
  <a href="https://github.com/weifuwan/seatunnel-web/issues">Issues</a>
</p>

---

## Overview

**SeaTunnel Web** is an independent third-party Web UI built for **Apache SeaTunnel**.

It provides a visual and practical way to create, configure, run, schedule, and monitor data synchronization jobs without manually maintaining complex SeaTunnel configuration files.

With SeaTunnel Web, users can manage data sources, build batch and streaming pipelines, configure field mappings, generate SeaTunnel job configurations, submit jobs to the SeaTunnel engine, inspect runtime logs, and monitor execution metrics from a unified Web interface.

> Our goal is simple: make Apache SeaTunnel easier to use in real-world data integration scenarios.

## Highlights

### Visual Pipeline Builder

Build data synchronization pipelines with a drag-and-drop DAG editor.

Configure Source, Transform, and Sink nodes visually, making complex synchronization workflows easier to understand and maintain.

### Batch and Streaming Jobs

Create and manage both batch and real-time data synchronization tasks through a unified interface.

SeaTunnel Web supports multiple task creation modes, including visual guidance and script-based configuration.

### Data Source Management

Manage commonly used data sources from one place, including:

* MySQL
* MySQL CDC
* PostgreSQL
* Oracle
* Other supported JDBC-compatible data sources

Users can configure connections, test connectivity, inspect metadata, and reuse data sources across different jobs.

### Field Mapping and Data Transformation

Configure source-to-target field mappings visually.

SeaTunnel Web also supports SQL-based transformations and automatically generates the corresponding SeaTunnel job configuration.

### Job Lifecycle Management

Manage the complete lifecycle of a SeaTunnel job:

* Create and edit jobs
* Publish job definitions
* Submit jobs
* Stop running jobs
* View execution history
* Inspect runtime logs
* Track job status
* Manage scheduled execution

### Runtime Metrics

View key runtime metrics directly from the Web UI, including:

* Read rows
* Written rows
* Read QPS
* Write QPS
* Data volume
* Job status
* Task execution progress

The built-in metrics view helps users understand job execution without requiring an additional monitoring platform for basic troubleshooting.

### Automatic Configuration Generation

SeaTunnel Web converts visual job definitions into executable SeaTunnel configuration files.

This reduces repetitive configuration work and helps teams standardize data synchronization development.

## Why SeaTunnel Web?

Apache SeaTunnel provides powerful data integration capabilities, but manually writing and maintaining configuration files can still be challenging in large-scale or multi-team environments.

SeaTunnel Web is designed for teams that need:

* A visual Web UI for Apache SeaTunnel
* Standardized data source management
* Low-code pipeline configuration
* Reusable synchronization workflows
* Batch and real-time job management
* Task scheduling and execution history
* Runtime logs and metrics
* Lower configuration and maintenance costs
* A smoother onboarding experience for new users

## Compatibility

The following environment is recommended for the current version:

| Component        | Supported or Recommended Version |
| ---------------- | -------------------------------- |
| Apache SeaTunnel | 2.3.13                           |
| Java             | JDK 21                           |
| Node.js          | 20 or later                      |
| npm              | Compatible with Node.js 20+      |
| MySQL            | MySQL 8.0 recommended            |
| Operating System | Linux recommended                |
| Browser          | Latest Chrome or Edge            |

> SeaTunnel Web currently performs version validation when connecting to the SeaTunnel engine. Please use a supported SeaTunnel version.

## Architecture

SeaTunnel Web uses a front-end and back-end separated architecture.

<img width="1448" height="1086" alt="31db05202fb68511127f1f6dcf367466" src="https://github.com/user-attachments/assets/187f2558-3668-4cc0-9ba8-9eb8807c3b02" />


## Quick Start

For complete installation and deployment instructions, please refer to the official project documentation:

**Documentation:**
https://doc.seatunnel-web.com/

### Prerequisites

Before starting SeaTunnel Web, prepare the following components:

1. Apache SeaTunnel 2.3.13
2. JDK 21
3. MySQL 8.0
4. Node.js 20 or later, only required when building the front end from source
5. Maven, or use the Maven Wrapper included in the repository

### 1. Clone the Repository

```bash
git clone https://github.com/weifuwan/seatunnel-web.git
cd seatunnel-web
```

### 2. Initialize the Database

Create the `seatunnel_web` database and execute the MySQL initialization script located in:

```text
seatunnel-web-api/src/main/resources/sql/
```

Before executing the script, review the SQL file and confirm that the database version is compatible.

### 3. Configure the Database

Update the back-end configuration:

```text
seatunnel-web-api/src/main/resources/application.yml
```

Example:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/seatunnel_web?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password
```

Do not use the example password in a production environment.

### 4. Build the Project

Build the complete project from the repository root:

```bash
./mvnw clean package -DskipTests
```

After a successful build, the distribution package will be generated under:

```text
seatunnel-web-dist/target/
```

The package contains:

```text
seatunnel-web-<version>/
├── bin/
├── conf/
├── libs/
├── sql/
├── web/
├── LICENSE
├── NOTICE
└── README.md
```

### 5. Start SeaTunnel Web

Extract the distribution package:

```bash
tar -zxvf seatunnel-web-<version>.tar.gz
cd seatunnel-web-<version>
```

Review the configuration under:

```text
conf/application.yml
```

Then start the service using the script under the `bin` directory.

After the service starts successfully, open:

```text
http://localhost:9527
```

The actual access address may vary depending on your reverse proxy and deployment configuration.

### 6. Connect to SeaTunnel

After logging in:

1. Open the SeaTunnel client management page.
2. Add a SeaTunnel 2.3.13 engine address.
3. Test the connection.
4. Create a data source.
5. Create and publish a synchronization job.
6. Submit the job and inspect runtime logs and metrics.

For detailed steps, see:

https://doc.seatunnel-web.com/

## Development

### Back-End Development

Requirements:

* JDK 21
* Maven 3.8 or later
* MySQL 8.0

Start the back end:

```bash
./mvnw clean install -DskipTests
./mvnw -pl seatunnel-web-api spring-boot:run
```

The default back-end port is:

```text
9527
```

### Front-End Development

Enter the front-end directory:

```bash
cd seatunnel-web-ui
```

Install dependencies:

```bash
yarn
```

Build the production assets:

```bash
yarn build
```

## Documentation

Detailed installation, configuration, operation, and usage guides are available at:

### SeaTunnel Web Documentation

https://doc.seatunnel-web.com/

The documentation covers topics such as:

* Environment preparation
* Database initialization
* SeaTunnel engine configuration
* Data source management
* Batch synchronization
* Streaming synchronization
* Workflow configuration
* Field mapping
* Task scheduling
* Runtime logs
* Metrics monitoring
* Troubleshooting

## Live Demo

An online demo environment is available at:

http://111.230.213.87:8000

The demo environment is intended for product preview and functional evaluation.

Please do not enter confidential, sensitive, or production data into the public demo environment.

## Roadmap

Planned improvements include:

* Additional data source plugins
* More SeaTunnel version compatibility
* Improved upgrade and database migration support
* Enhanced job validation
* Alert and notification capabilities
* More complete operational monitoring
* Improved permission management
* Better internationalization
* Docker and containerized deployment support

Roadmap priorities may change based on community feedback and actual usage scenarios.

## Known Limitations

Before using the current version, please note:

* The currently validated SeaTunnel version is 2.3.13.
* MySQL 8.0 is recommended for the SeaTunnel Web metadata database.
* Some advanced SeaTunnel connector parameters may still require script-mode configuration.
* Production deployment should use a reverse proxy and secure database credentials.
* The public demo environment must not be used with sensitive data.
* Back up the SeaTunnel Web database before upgrading to a newer version.

Please review open issues before deploying the project in a production environment:

https://github.com/weifuwan/seatunnel-web/issues

## Contributing

Contributions are warmly welcome.

You can contribute by:

* Reporting bugs
* Submitting feature requests
* Improving documentation
* Adding data source plugins
* Fixing issues
* Improving test coverage
* Sharing deployment experience
* Helping other community users

Recommended contribution workflow:

1. Fork the repository.
2. Create a feature branch.
3. Make and test your changes.
4. Submit a pull request.
5. Describe the motivation, implementation, and verification process clearly.

Repository:

https://github.com/weifuwan/seatunnel-web

Issues:

https://github.com/weifuwan/seatunnel-web/issues

Pull requests:

https://github.com/weifuwan/seatunnel-web/pulls

## Community

If you are interested in SeaTunnel Web, want to share feedback, or would like to participate in its development, you are welcome to join the community.

Contributions are not limited to writing code. Documentation, testing, issue reports, feature discussions, product suggestions, and usage experience are all valuable.

<p align="center">
  <img
    width="200"
    height="320"
    src="https://github.com/user-attachments/assets/41de5095-91af-41e6-9345-7c26496f9469"
    alt="SeaTunnel Web Community Group"
  />
</p>

<p align="center">
  Join the SeaTunnel Web community and help build the project together.
</p>

## Security

Please do not disclose security vulnerabilities through public GitHub issues.

When reporting a security issue, include:

* The affected version
* The affected component
* Reproduction steps
* Potential impact
* Suggested remediation, when available

A dedicated security reporting process will be documented in `SECURITY.md`.

## License

SeaTunnel Web is licensed under the Apache License 2.0.

See the [LICENSE](./LICENSE) file for details.

## Disclaimer

SeaTunnel Web is an independent third-party project.

It is not an official Apache Software Foundation project and is not affiliated with or endorsed by the Apache Software Foundation.

Apache SeaTunnel, SeaTunnel, Apache, and the Apache feather logo are trademarks of the Apache Software Foundation.

The use of Apache SeaTunnel in this project name and documentation is intended only to describe compatibility and integration with Apache SeaTunnel.

---

<p align="center">
  Made with ❤️ by the SeaTunnel Web community
</p>

<p align="center">
  <a href="https://github.com/weifuwan/seatunnel-web">GitHub</a>
  ·
  <a href="https://doc.seatunnel-web.com/">Documentation</a>
  ·
  <a href="https://github.com/weifuwan/seatunnel-web/issues">Feedback</a>
</p>
