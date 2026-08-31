package com.VanControl.VanControl.user.service;

import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User buscarUsuarioPorCpf(String cpf) {
        var user = userRepository.findByCpf(cpf);
        if (user == null) {
            throw new NotFoundException("Usuário não encontrado");
        }
        return user;
    }
}
