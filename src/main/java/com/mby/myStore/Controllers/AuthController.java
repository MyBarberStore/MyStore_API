package com.mby.myStore.Controllers;

import com.mby.myStore.DTO.LoginRequest;
import com.mby.myStore.DTO.LoginResponse;
import com.mby.myStore.DTO.UserResponse;
import com.mby.myStore.Model.User;
import com.mby.myStore.Security.JwtService;
import com.mby.myStore.Services.UserService;
import com.mby.myStore.Utils.HashPsw;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
@Tag(name = "Autenticación", description = "Endpoints del sistema de autenticación. Login y register")
public class AuthController {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Inicio de sesión", description = "Inicia sesión y devuelve el usuario y token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Éxito"),
            @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos"),
            @ApiResponse(responseCode = "404", description = "Email no registrado")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginData) {
        // Comprobar email y password en la DB
        UserResponse user = userService.login(loginData.getEmail(), loginData.getPassword());

        // Si ha llegado aquí es que el login es correcto. Generamos la "llave" (token)
        // Usamos el email como identificador único en el token
        String token = jwtService.generateToken(user);

        // Devolvemos TODO: el token para que Android lo guarde y el cliente para la UI
        return ResponseEntity.ok(new LoginResponse(token, user));
    }

    /**
     * Registro de un nuevo cliente.
     * Al ser una ruta /auth, no requerirá token en SecurityConfig.
     */
    @Operation(summary = "Registro de cliente", description = "Crea un nuevo usuario y devuelve el token de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado y logueado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "El email o usuario ya está registrado")
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registrarCliente(@RequestBody User user) {
        user.setPassword(HashPsw.hashPassword(user.getPassword())); //hash de password
        // Guardamos el cliente usando el servicio que ya tienes
        userService.addUser(user);
        //generar el token directamente para que
        //tras registrarse entre automáticamente a la App
        UserResponse userdto = userService.entityToDTO(user);
        String token = jwtService.generateToken(userdto);

        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(token, userdto));
    }
}
