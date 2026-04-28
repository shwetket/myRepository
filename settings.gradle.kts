rootProject.name = "myRepository"

// Backend domain modules
include(
    "application:finance",
    "application:health"
)

// Shared + infrastructure
include(
    "infrastructure",
    "shared"
)