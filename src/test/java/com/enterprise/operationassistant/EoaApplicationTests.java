package com.enterprise.operationassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:eoa-test;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password="
    })
class EoaApplicationTests {

  @Autowired private WebApplicationContext applicationContext;

  @Test
  void healthEndpointReturnsUp() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

    var result = mockMvc.perform(get("/actuator/health")).andReturn();

    assertEquals(200, result.getResponse().getStatus());
    assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"UP\""));
  }
}
