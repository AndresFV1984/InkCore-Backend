package com.inkcore.application.user.usecase;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {

    private final UserRepositoryPort userRepository;

    public ListUsersUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param state {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<User> execute(Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        if (state == null) {
            return userRepository.findPage(query);
        }
        return userRepository.findPageByState(state, query);
    }
}
