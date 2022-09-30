package com.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.cursoandroid.uber.R;
import com.cursoandroid.uber.config.ConfiguracaoFirebase;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastroActivity extends AppCompatActivity {
    private TextInputEditText et_nome, et_email, et_senha;
    private Switch sw_tipo_usuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        et_nome = findViewById(R.id.et_nome_cadastro);
        et_email = findViewById(R.id.et_email_cadastro);
        et_senha = findViewById(R.id.et_senha_cadastro);
        sw_tipo_usuario = findViewById(R.id.sw_tipo_usuario);
    }

    public void validarCadastroUsuario(View view) {
        String nome = et_nome.getText().toString();
        String email = et_email.getText().toString();
        String senha = et_senha.getText().toString();

        if (!nome.isEmpty()) {
            if (!email.isEmpty()) {
                if (!senha.isEmpty()) {
                    Usuario usuario = new Usuario();
                    usuario.setNome(nome);
                    usuario.setEmail(email);
                    usuario.setSenha(senha);
                    usuario.setTipo(verificaTipoUusario());

                    cadastrarUsuario(usuario); //salva os dados no banco

                } else {
                    Toast.makeText(CadastroActivity.this, "Preencha a senha!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CadastroActivity.this, "Preencha o email!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CadastroActivity.this, "Preencha o nome!", Toast.LENGTH_SHORT).show();
        }
    }


    private String verificaTipoUusario() {
        return sw_tipo_usuario.isChecked() ? "M" : "P";
    }

    public void cadastrarUsuario(Usuario usuario) {
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    try {

                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Atualizar nome no profile
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                        if (verificaTipoUusario() == "P") {
                            startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar passageiro",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar motorista",
                                    Toast.LENGTH_SHORT).show();

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String excecao = "";
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        excecao = "Digite um senha mais forte";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        excecao = "Por favor, digite um e-mail válido";
                    } catch (FirebaseAuthUserCollisionException e) {
                        excecao = "Esta conta já foi cadastrada!";
                    } catch (Exception e) {
                        excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(CadastroActivity.this, excecao, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}