package com.piloto.cdi.gateway.governance.config;

import com.piloto.cdi.gateway.governance.service.PromptGovernanceEngine;
import com.piloto.cdi.kernel.governance.model.PromptEntity;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import com.piloto.cdi.kernel.governance.type.PromptRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bootstraps the in-memory PromptRegistry with default system prompts.
 * This ensures the system works out-of-the-box in Phase 1.
 */
@Configuration
public class GovernanceBootstrap {

    @Bean
    public CommandLineRunner initPrompts(PromptGovernanceEngine pge) {
        return args -> {
            // Seed PROPOSER
            pge.registerPrompt(PromptEntity.builder()
                    .name("Default Proposer Core")
                    .layer(PromptLayer.ROLE)
                    .role(PromptRole.PROPOSER)
                    .content(
                            "Eres el PROPOSER de un consejo deliberativo. Tu objetivo es proponer un plan de acción y declarar supuestos explícitos.\n"
                                    +
                                    "Responde ÚNICAMENTE en formato JSON con la siguiente estructura: \n" +
                                    "{\"plan\": \"...\", \"assumptions\": [\"...\"], \"claims\": [{\"statement\": \"...\", \"requires_grounding\": true}], \"risk_level\": \"LOW|MEDIUM|HIGH\"}")
                    .version(1)
                    .isActive(true)
                    .build());

            // Seed CRITIC
            pge.registerPrompt(PromptEntity.builder()
                    .name("Default Critic Core")
                    .layer(PromptLayer.ROLE)
                    .role(PromptRole.CRITIC)
                    .content(
                            "Eres el CRITIC de un consejo deliberativo. Busca fallas lógicas, ambigüedades y supuestos ocultos.\n"
                                    +
                                    "Responde ÚNICAMENTE en JSON: \n" +
                                    "{\"logical_issues\": [\"...\"], \"unsupported_claims\": [\"...\"], \"hidden_assumptions\": [\"...\"], \"risk_escalation\": false, \"confidence_in_plan\": 0.8}")
                    .version(1)
                    .isActive(true)
                    .build());

            // Seed VERIFIER
            pge.registerPrompt(PromptEntity.builder()
                    .name("Default Verifier Core")
                    .layer(PromptLayer.ROLE)
                    .role(PromptRole.VERIFIER)
                    .content(
                            "Eres el VERIFIER. Verifica afirmaciones factuales contra el contexto original y las Reglas de Negocio/Políticas inyectadas.\n"
                                    +
                                    "Responde ÚNICAMENTE en JSON: \n" +
                                    "{\"grounding_score\": 0.9, \"contradictions_found\": [], \"unsupported_claims\": [], \"factual_risk\": \"LOW\"}")
                    .version(1)
                    .isActive(true)
                    .build());

            // Seed ARBITER
            pge.registerPrompt(PromptEntity.builder()
                    .name("Default Arbiter Core")
                    .layer(PromptLayer.ROLE)
                    .role(PromptRole.ARBITER)
                    .content("Eres el ARBITER. Toma la decisión final basada en el consejo.\n" +
                            "Responde ÚNICAMENTE en JSON: \n" +
                            "{\"decision\": \"APPROVED|REVISION_REQUIRED|REJECTED\", \"reasoning_score\": 0.8, \"unsupported_claims_count\": 0, \"final_risk\": \"LOW\"}")
                    .version(1)
                    .isActive(true)
                    .build());

            System.out.println("Governance Layer Bootstrapped: Default Prompts Loaded.");
        };
    }
}
