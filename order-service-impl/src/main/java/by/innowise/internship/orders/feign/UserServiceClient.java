package by.innowise.internship.orders.feign;

import by.innowise.common.library.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userServiceClient", url = "${application.feign.clients.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/v1/internal/users/{userId}")
    UserProfileDto getUserById(@PathVariable Long userId);

}
