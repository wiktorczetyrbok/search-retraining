package com.griddynamics.productindexer.common;

import org.junit.Test;

import static org.hamcrest.Matchers.*;

public class CapstoneIntegrationTest extends BaseTest {

    private final APIClient client = new APIClient();

    // 8.3.1 Empty responses

    @Test
    public void testEmptyRequestReturnsEmptyResponse() {
        client.productRequest()
                .body("{}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(0));
    }

    @Test
    public void testSearchWithWrongWordReturnsEmptyResponse() {
        client.productRequest()
                .body("{\"textQuery\": \"Calvin klein L blue ankle skinny jeans wrongword\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(0));
    }

    @Test
    public void testSearchWithNonMatchingColorReturnsEmptyResponse() {
        client.productRequest()
                .body("{\"textQuery\": \"Calvin klein L red ankle skinny jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(0));
    }

    // 8.3.2 Happy path

    @Test
    public void testExactProductMatchReturnsExpectedProduct() {
        client.productRequest()
                .body("{\"textQuery\": \"Calvin klein L blue ankle skinny jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(1))
                .body("products", hasSize(1))
                .body("products[0].id", is("2"))
                .body("products[0].brand", is("Calvin Klein"))
                .body("products[0].name", containsString("Women ankle skinny jeans"))
                .body("products[0].skus", hasSize(9))
                .body("facets.brand", notNullValue())
                .body("facets.price", notNullValue())
                .body("facets.color", notNullValue())
                .body("facets.size", notNullValue());
    }

    // 8.3.3.1

    @Test
    public void testFacetsWithJeansQuery() {
        client.productRequest()
                .body("{\"textQuery\": \"jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("facets.brand[0].value", is("Calvin Klein"))
                .body("facets.brand[0].count", is(4))
                .body("facets.brand[1].value", is("Levi's"))
                .body("facets.brand[1].count", is(4))
                .body("facets.price[0].value", is("Cheap"))
                .body("facets.price[0].count", is(2))
                .body("facets.price[1].value", is("Average"))
                .body("facets.price[1].count", is(6))
                .body("facets.price[2].value", is("Expensive"))
                .body("facets.price[2].count", is(0))
                .body("facets.color", hasSize(4))
                .body("facets.size", hasSize(6));
    }

    // 8.3.3.2

    @Test
    public void testFacetsWithWomenAnkleBlueJeans() {
        client.productRequest()
                .body("{\"textQuery\": \"women ankle blue jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("facets.brand", hasSize(2))
                .body("facets.price[1].count", is(3))
                .body("facets.color[0].value", is("Black"))
                .body("facets.size", hasSize(greaterThan(0)));
    }

    // 8.3.4

    @Test
    public void testSortWithJeansQuery() {
        client.productRequest()
                .body("{\"textQuery\": \"jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("products[0].id", is("8"))
                .body("products[1].id", is("7"))
                .body("products[7].id", is("1"));
    }

    @Test
    public void testBoostWithBlueWomenJeans() {
        client.productRequest()
                .body("{\"textQuery\": \"blue WOMEN jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(5))
                .body("products[0].id", is("5"));
    }

    @Test
    public void testBoostWithWomenBlueJeans() {
        client.productRequest()
                .body("{\"textQuery\": \"WOMEN blue jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("products[0].id", is("6"));
    }

    @Test
    public void testBoostWithWomenAnkleBlueJeans() {
        client.productRequest()
                .body("{\"textQuery\": \"women ankle blue jeans\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(3))
                .body("products[0].id", is("6"));
    }

    // 8.3.5

    @Test
    public void testPaginationWithJeansQueryPage1() {
        client.productRequest()
                .body("{\"textQuery\": \"jeans\", \"size\": 2, \"page\": 1}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", is(8))
                .body("products", hasSize(2))
                .body("products[0].id", is("6"))
                .body("products[1].id", is("5"));
    }
}
