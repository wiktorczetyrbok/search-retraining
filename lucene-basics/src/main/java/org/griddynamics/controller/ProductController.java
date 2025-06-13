package org.griddynamics.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.griddynamics.model.SearchRequest;
import org.griddynamics.model.SearchResponse;
import org.griddynamics.service.ProductService;

@Path("/lucene/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

    @Inject
    ProductService productService;

    @POST
    @Path("/index")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createIndex() {
        Integer indexedDocs = productService.createIndex();
        return Response.ok("Indexed " + indexedDocs + " documents.").build();
    }


    @POST
    @Path("/search")
    public SearchResponse getSearchServiceResponse(SearchRequest request) {
        return productService.searchProducts(request);
    }

    @DELETE
    @Path("/{productId}")
    public Response deleteProduct(@PathParam("productId") String productId) {
        productService.deleteProduct(productId);
        return Response.noContent().build();
    }

}
