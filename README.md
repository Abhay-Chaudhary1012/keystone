\# KEYSTONE



\## Meridian Facilities Management Platform



KEYSTONE is a full-stack field-service management platform designed for Meridian Facilities Management. It helps manage service requests, work orders, technician assignments, job execution, parts usage, technician time, and operational reporting through role-based workflows.



\## Project Overview



The platform connects customers, dispatchers, technicians, and managers through a centralized system.



\### Core Workflow



Customer Service Request  

→ Work Order Creation  

→ Dispatcher Assignment  

→ Technician Execution  

→ Parts \& Time Logging  

→ Work Order Completion  

→ Management Reporting



\## User Roles



| Role | Responsibilities |

|------|------------------|

| \*\*CUSTOMER\*\* | Create service requests and view their work orders |

| \*\*DISPATCHER\*\* | Create, assign, update, cancel, and monitor work orders |

| \*\*TECHNICIAN\*\* | View assigned work orders, execute jobs, log parts and time, and update job status |

| \*\*MANAGER\*\* | Monitor work orders, workflows, and operational metrics |



\## Work Order Lifecycle



```text

OPEN

&#x20;├── ASSIGNED

&#x20;│     └── IN\_PROGRESS

&#x20;│            ├── ON\_HOLD

&#x20;│            │     └── IN\_PROGRESS

&#x20;│            └── COMPLETED

&#x20;└── CANCELLED

