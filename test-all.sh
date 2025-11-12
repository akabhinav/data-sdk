#!/bin/bash

# DataVerse SDK - Automated Test Script
# This script sets up and runs all tests for DataVerse SDK features

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}          DataVerse SDK - Automated Test Suite               ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_section() {
    echo -e "\n${YELLOW}━━━ $1 ━━━${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

check_prerequisites() {
    print_section "Checking Prerequisites"

    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 21 ]; then
            print_success "Java $JAVA_VERSION installed"
        else
            print_error "Java 21+ required, found version $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 21+"
        exit 1
    fi

    # Check Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
        print_success "Maven $MVN_VERSION installed"
    else
        print_error "Maven not found. Please install Maven 3.8+"
        exit 1
    fi

    # Check Docker (optional)
    if command -v docker &> /dev/null; then
        print_success "Docker installed (optional databases available)"
        DOCKER_AVAILABLE=true
    else
        print_info "Docker not found (optional, for database testing)"
        DOCKER_AVAILABLE=false
    fi

    # Check Docker Compose (optional)
    if command -v docker-compose &> /dev/null; then
        print_success "Docker Compose installed"
        COMPOSE_AVAILABLE=true
    else
        print_info "Docker Compose not found (optional)"
        COMPOSE_AVAILABLE=false
    fi
}

start_databases() {
    if [ "$DOCKER_AVAILABLE" = true ] && [ "$COMPOSE_AVAILABLE" = true ]; then
        print_section "Starting Test Databases"

        if [ -f "docker-compose.yml" ]; then
            print_info "Starting PostgreSQL, MongoDB, and Redis..."
            docker-compose up -d

            print_info "Waiting for databases to be ready..."
            sleep 10

            # Check PostgreSQL
            if docker-compose exec -T postgres pg_isready -U dataverse &> /dev/null; then
                print_success "PostgreSQL is ready"
            else
                print_error "PostgreSQL failed to start"
            fi

            # Check MongoDB
            if docker-compose exec -T mongodb mongosh --eval "db.version()" &> /dev/null; then
                print_success "MongoDB is ready"
            else
                print_error "MongoDB failed to start"
            fi

            # Check Redis
            if docker-compose exec -T redis redis-cli ping &> /dev/null; then
                print_success "Redis is ready"
            else
                print_error "Redis failed to start"
            fi
        else
            print_info "docker-compose.yml not found, skipping database setup"
        fi
    else
        print_info "Skipping database setup (Docker not available)"
    fi
}

build_project() {
    print_section "Building Project"

    print_info "Running: mvn clean install -DskipTests"
    if mvn clean install -DskipTests > build.log 2>&1; then
        print_success "Project built successfully"
    else
        print_error "Build failed. Check build.log for details"
        tail -n 20 build.log
        exit 1
    fi
}

run_unit_tests() {
    print_section "Running Unit Tests"

    print_info "Running all unit tests..."
    if mvn test > test.log 2>&1; then
        # Extract test results
        TESTS_RUN=$(grep "Tests run:" test.log | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
        FAILURES=$(grep "Tests run:" test.log | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
        ERRORS=$(grep "Tests run:" test.log | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')

        print_success "Tests run: $TESTS_RUN"

        if [ "$FAILURES" = "0" ] && [ "$ERRORS" = "0" ]; then
            print_success "All tests passed!"
        else
            print_error "Failures: $FAILURES, Errors: $ERRORS"
            grep -A 5 "FAILURE" test.log
            exit 1
        fi
    else
        print_error "Tests failed. Check test.log for details"
        tail -n 30 test.log
        exit 1
    fi
}

run_examples() {
    print_section "Running Examples"

    cd dataverse-examples

    EXAMPLES=(
        "ReadReplicasExample"
        "NativeQueryExample"
        "LazyEagerLoadingExample"
        "SchemaMigrationExample"
        "DistributedTracingExample"
        "CrossDatasourceJoinsExample"
        "DataSynchronizationExample"
        "HotReloadExample"
        "GraphQLExample"
    )

    for example in "${EXAMPLES[@]}"; do
        print_info "Running $example..."
        if mvn exec:java -Dexec.mainClass="io.dataverse.examples.$example" -q > "../example_$example.log" 2>&1; then
            print_success "$example completed"
        else
            print_error "$example failed. Check example_$example.log"
        fi
    done

    cd ..
}

test_feature() {
    local feature_num=$1
    local feature_name=$2
    local test_class=$3
    local example_class=$4

    print_section "Testing Feature #$feature_num: $feature_name"

    # Run unit test
    print_info "Running unit tests: $test_class"
    if mvn test -Dtest="$test_class" -q > "feature_${feature_num}_test.log" 2>&1; then
        print_success "Unit tests passed"
    else
        print_error "Unit tests failed"
        return 1
    fi

    # Run example
    print_info "Running example: $example_class"
    cd dataverse-examples
    if mvn exec:java -Dexec.mainClass="io.dataverse.examples.$example_class" -q > "../feature_${feature_num}_example.log" 2>&1; then
        print_success "Example completed"
        cd ..
        return 0
    else
        print_error "Example failed"
        cd ..
        return 1
    fi
}

test_all_features() {
    print_section "Testing All Features"

    local passed=0
    local failed=0

    # Feature #9
    if test_feature 9 "Read Replicas" "ReplicaRouterTest,ReplicaHealthCheckerTest" "ReadReplicasExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #3
    if test_feature 3 "Native Query" "ParameterBinderTest" "NativeQueryExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #8
    if test_feature 8 "Lazy/Eager Loading" "FetchPlanTest" "LazyEagerLoadingExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #16
    if test_feature 16 "Schema Migration" "MigrationManagerTest" "SchemaMigrationExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #14
    if test_feature 14 "Distributed Tracing" "TraceContextTest,TracerTest" "DistributedTracingExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #5
    if test_feature 5 "Cross-Datasource Joins" "JoinBuilderTest" "CrossDatasourceJoinsExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #17
    if test_feature 17 "Data Synchronization" "SyncManagerTest" "DataSynchronizationExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #20
    if test_feature 20 "Hot Reload" "HotReloadManagerTest" "HotReloadExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    # Feature #19
    if test_feature 19 "GraphQL" "GraphQLSchemaGeneratorTest,GraphQLExecutorTest" "GraphQLExample"; then
        ((passed++))
    else
        ((failed++))
    fi

    print_section "Feature Test Results"
    print_success "Passed: $passed/9 features"
    if [ $failed -gt 0 ]; then
        print_error "Failed: $failed/9 features"
    fi
}

cleanup() {
    if [ "$DOCKER_AVAILABLE" = true ] && [ "$COMPOSE_AVAILABLE" = true ]; then
        print_section "Cleanup"

        read -p "Stop Docker containers? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_info "Stopping databases..."
            docker-compose down
            print_success "Databases stopped"
        fi
    fi
}

print_summary() {
    print_section "Test Summary"

    echo "Logs generated:"
    echo "  - build.log          : Build output"
    echo "  - test.log           : Unit test results"
    echo "  - example_*.log      : Example outputs"
    echo "  - feature_*_*.log    : Feature-specific tests"
    echo ""

    print_success "All tests completed!"
    echo ""
    echo "Next steps:"
    echo "  1. Review logs for any issues"
    echo "  2. Run specific examples: mvn exec:java -Dexec.mainClass=..."
    echo "  3. Integrate features into your application"
    echo ""
}

# Main execution
main() {
    print_header

    check_prerequisites

    # Menu
    echo "Select test mode:"
    echo "  1) Full test suite (build + unit tests + examples)"
    echo "  2) Unit tests only"
    echo "  3) Examples only"
    echo "  4) Test specific feature"
    echo "  5) Quick test (build + unit tests)"
    read -p "Enter choice [1-5]: " choice

    case $choice in
        1)
            start_databases
            build_project
            run_unit_tests
            run_examples
            test_all_features
            cleanup
            print_summary
            ;;
        2)
            build_project
            run_unit_tests
            ;;
        3)
            build_project
            run_examples
            ;;
        4)
            build_project
            read -p "Enter feature number [1-9]: " feature_num
            case $feature_num in
                1) test_feature 9 "Read Replicas" "ReplicaRouterTest" "ReadReplicasExample" ;;
                2) test_feature 3 "Native Query" "ParameterBinderTest" "NativeQueryExample" ;;
                3) test_feature 8 "Lazy/Eager Loading" "FetchPlanTest" "LazyEagerLoadingExample" ;;
                4) test_feature 16 "Schema Migration" "MigrationManagerTest" "SchemaMigrationExample" ;;
                5) test_feature 14 "Distributed Tracing" "TraceContextTest" "DistributedTracingExample" ;;
                6) test_feature 5 "Cross-Datasource Joins" "JoinBuilderTest" "CrossDatasourceJoinsExample" ;;
                7) test_feature 17 "Data Synchronization" "SyncManagerTest" "DataSynchronizationExample" ;;
                8) test_feature 20 "Hot Reload" "HotReloadManagerTest" "HotReloadExample" ;;
                9) test_feature 19 "GraphQL" "GraphQLSchemaGeneratorTest" "GraphQLExample" ;;
                *) print_error "Invalid feature number" ;;
            esac
            ;;
        5)
            build_project
            run_unit_tests
            print_success "Quick test completed!"
            ;;
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
}

# Run main
main
