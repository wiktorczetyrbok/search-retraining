package com.griddynamics.esgraduationproject.common;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static io.restassured.RestAssured.given;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = { "classpath:integration-test.properties" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, MockitoTestExecutionListener.class })
public abstract class BaseTest {

    @LocalServerPort
    private int port;

    public int getSpringBootPort() {
        return port;
    }

    protected class APIClient {

        private boolean logResponse = false;

        /**
         * Use this method to log the response to debug tests
         */
        public APIClient logResponse() {
            this.logResponse = true;
            return this;
        }

        public RequestSpecification typeaheadRequest() {
            return baseRequest()
                .basePath("/v1/typeahead")
                .header("Content-Type", "application/json");
        }

        public RequestSpecification baseRequest() {
            RequestSpecification requestSpecification = given()
                .baseUri("http://localhost").port(getSpringBootPort())
                .log().all();

            if (logResponse) {
                requestSpecification = requestSpecification.filter(new ResponseLoggingFilter());
            }

            return requestSpecification;
        }

    }
}
