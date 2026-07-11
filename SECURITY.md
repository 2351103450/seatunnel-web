# Security Policy

SeaTunnel Web takes security issues seriously. Please report suspected vulnerabilities privately so maintainers can investigate and coordinate a fix before public disclosure.

## Supported Versions

Security fixes are provided for actively maintained release lines. Until the first stable release is published, security fixes target the `main` branch.

| Version | Supported |
| ------- | --------- |
| main    | Yes       |
| < 1.0.0 | No        |

## Reporting a Vulnerability

Please do **not** create a public GitHub issue for security vulnerabilities.

Instead, report issues through the Apache Software Foundation security process:

- Email: `security@apache.org`
- Include `SeaTunnel Web` in the subject line.
- Provide a description, affected versions or commits, reproduction steps, impact, and any suggested mitigation.

The project will acknowledge reports as soon as possible and will coordinate disclosure according to ASF security practices.

## Security Best Practices for Deployments

- Change default credentials before exposing an environment.
- Restrict access to the Web UI and API with network controls and authentication.
- Store datasource credentials securely and rotate them regularly.
- Keep SeaTunnel Web, Apache SeaTunnel, JDK, Node.js, and database drivers up to date.
- Review logs and audit trails for unexpected activity.
