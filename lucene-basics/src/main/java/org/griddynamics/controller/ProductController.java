package org.griddynamics.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.griddynamics.model.Product;
import org.griddynamics.model.ProductSearchRequest;
import org.griddynamics.model.ProductSearchResponse;
import org.griddynamics.service.ProductService;


@Path("/lucene-basics/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

    @Inject
    ProductService productService;

    @POST
    @Path("/search")
    public ProductSearchResponse getSearchServiceResponse(ProductSearchRequest request) {
        return productService.searchProducts(request);
    }

    @DELETE
    @Path("/{productId}")
    public Response deleteProduct(@PathParam("productId") String productId) {
        productService.deleteProduct(productId);
        return Response.noContent().build();
    }

    @POST
    public Response updateProduct(Product product) {
        productService.updateProduct(product);
        return Response.noContent().build();
    }

    @POST
    @Path("/index")
    public Response createIndex() {
        productService.createIndex();
        return Response.noContent().build();
    }
}
