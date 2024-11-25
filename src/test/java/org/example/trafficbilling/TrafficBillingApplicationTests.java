package org.example.trafficbilling;

import org.example.trafficbilling.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
public class TrafficBillingApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RateLimiterService rateLimiterService;

	@Test
	public void testHighConcurrency() throws Exception {
		String[] users = {"user1", "user2", "user3", "user4"};
		String[] apis = {"/traffic/api1", "/traffic/api2", "/traffic/api3"};
		int requestsPerSecond = 500; // 每秒500次请求
		int testDurationSeconds = 60; // 测试时长60秒
		int limitPerMinute = 10000; // 每分钟限制10000次请求

		ExecutorService executorService = Executors.newFixedThreadPool(100);
		CountDownLatch latch = new CountDownLatch(users.length * requestsPerSecond * testDurationSeconds);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger rateLimitCount = new AtomicInteger(0);
		ConcurrentLinkedQueue<Long> statTimes = new ConcurrentLinkedQueue<>();

		Random random = new Random();

		for (String user : users) {
			for (int i = 0; i < requestsPerSecond * testDurationSeconds; i++) {
				executorService.submit(() -> {
					try {
						// 随机选择 API 类型
						String api = apis[random.nextInt(apis.length)];
						MockHttpServletRequestBuilder request = switch (api) {
                            case "/traffic/api2" -> // POST 请求
                                    MockMvcRequestBuilders.post(api)
                                            .header("userId", user)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"key\":\"value\"}");
                            case "/traffic/api3" -> // PUT 请求
                                    MockMvcRequestBuilders.put(api)
                                            .header("userId", user)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"key\":\"newValue\"}");
                            default -> // GET 请求
                                    MockMvcRequestBuilders.get(api)
                                            .header("userId", user)
                                            .param("param1", "value1");
                        };

						long statStart = System.nanoTime();

						// 发送请求并统计响应状态
						MvcResult result = mockMvc.perform(request).andReturn();

						long statEnd = System.nanoTime();
						statTimes.add(statEnd - statStart);

						int status = result.getResponse().getStatus();
						if (status == HttpStatus.OK.value()) {
							successCount.incrementAndGet();
						} else if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
							rateLimitCount.incrementAndGet();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						latch.countDown();
					}
				});
			}
		}

		// 等待所有请求完成
		latch.await();
		executorService.shutdown();

		// 统计流量统计逻辑耗时
		long totalStatTime = statTimes.stream().mapToLong(Long::longValue).sum();
		double averageStatTimeMicro = (double) totalStatTime / statTimes.size() / 1000 / 1000; // 毫秒

		System.out.println("===============================");
		System.out.println("Total Requests: " + (users.length * requestsPerSecond * testDurationSeconds));
		System.out.println("Successful Requests: " + successCount.get());
		System.out.println("Rate Limited Requests: " + rateLimitCount.get());
		System.out.println("Average Traffic Stat Time: " + averageStatTimeMicro + " ms");
		System.out.println("===============================");

		// 验证限流效果
		int expectedMaxSuccess = users.length * limitPerMinute;
		assertTrue(successCount.get() <= expectedMaxSuccess,
				"Success count should not exceed the per-minute limit.");

		// 确保部分请求被限流
		assertTrue(rateLimitCount.get() > 0, "Some requests should be rate limited.");

		// 确保流量统计逻辑耗时在合理范围内，例如平均小于 10ms
		assertTrue(averageStatTimeMicro < 10, "Traffic stat logic is too slow!");
	}

}
