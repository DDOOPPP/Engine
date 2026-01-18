package org.gi.gIEngine.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.gi.builder.DamageSourceBuilder;
import org.gi.damage.DamageType;
import org.gi.damage.IDamageCalculator;
import org.gi.damage.IDamageResult;
import org.gi.damage.IDamageSource;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.gIEngine.service.PlayerStatManager;

public class DamageListener implements Listener {
    private final PlayerStatManager statManager;
    private final IDamageCalculator damageCalculator;

    public DamageListener(PlayerStatManager statManager, IDamageCalculator damageCalculator){
        this.statManager = statManager;
        this.damageCalculator = damageCalculator;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event){
        Entity targetEntity = event.getEntity();
        Entity attackerEntity = getActualDamager(event);

        // 공격자 홀더 (플레이어만)
        PlayerStatHolder attacker = null;
        if (attackerEntity instanceof Player attackerPlayer) {
            attacker = statManager.getHolderIfReady(attackerPlayer).orElse(null);
        }

        // 피격자 홀더 (플레이어만)
        PlayerStatHolder target = null;
        if (targetEntity instanceof Player targetPlayer) {
            target = statManager.getHolderIfReady(targetPlayer).orElse(null);
        }

        // 둘 다 없으면 바닐라 데미지 유지
        if (attacker == null && target == null) {
            return;
        }

        DamageType damageType = mapDamageType(event.getCause());
        double baseDamage = event.getDamage();

        boolean canEvadeOrBlock = (target != null) && (damageType != DamageType.TRUE);

        IDamageSource source = DamageSourceBuilder.create()
                .attacker(attacker)
                .damageType(damageType)
                .baseDamage(baseDamage)
                .skillScaling(attacker != null ? 1.0 : 0)
                .displayName(getDisplayName(event))
                .canCritical(attacker != null)
                .canBlock(canEvadeOrBlock)
                .canEvade(canEvadeOrBlock)
                .build();

        IDamageResult result = damageCalculator.calculate(source, target);
        event.setDamage(result.getFinalDamage());

        // 메시지 전송
        sendDamageMessage(targetEntity, attackerEntity, result);
    }

    private DamageType mapDamageType(EntityDamageEvent.DamageCause cause){
        return switch (cause){
            case ENTITY_ATTACK,
                 ENTITY_SWEEP_ATTACK,
                 PROJECTILE,
                 THORNS -> DamageType.PHYSICAL;

            case MAGIC,
                 DRAGON_BREATH,
                 WITHER,
                 POISON -> DamageType.MAGICAL;

            case FIRE,
                 FIRE_TICK,
                 LAVA,
                 HOT_FLOOR,
                 DROWNING,
                 SUFFOCATION,
                 FALL,
                 VOID,
                 LIGHTNING,
                 STARVATION,
                 FREEZE,
                 SONIC_BOOM
                -> DamageType.TRUE;

            default -> DamageType.PHYSICAL;
        };
    }

    private String getSourceID(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();
        String prefix = "entity";

        if (damager instanceof Player){
            prefix = "player";
        } else if (damager instanceof Projectile) {
            prefix = "projectile";
        }

        return prefix + ":" + damager.getType().name().toLowerCase();
    }

    private String getDisplayName(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();

        if (damager instanceof Player player){
            return player.getDisplayName();
        }

        return damager.getType().name();
    }

    private void sendDamageMessage(Entity victim, Entity damager, IDamageResult result) {
        // 피해자가 플레이어면 메시지
        if (victim instanceof Player victimPlayer) {
            String message = buildDamageMessage(result, false);
            victimPlayer.sendMessage(message);
        }

        // 공격자가 플레이어면 메시지
        if (damager instanceof Player attackerPlayer) {
            String attackerMessage = buildDamageMessage(result, true);
            attackerPlayer.sendMessage(attackerMessage);
        }
    }

    /**
     * 데미지 메시지 생성
     */
    private String buildDamageMessage(IDamageResult result, boolean isAttacker) {
        StringBuilder sb = new StringBuilder();

        ChatColor color = switch (result.getResultType()) {
            case CRITICAL -> ChatColor.GOLD;
            case EVADED -> ChatColor.GRAY;
            case BLOCKED, FULL_BLOCKED -> ChatColor.AQUA;
            default -> ChatColor.RED;
        };

        if (isAttacker) {
            sb.append(ChatColor.GRAY).append("[데미지] ");
        } else {
            sb.append(ChatColor.GRAY).append("[피해] ");
        }

        // 회피
        if (result.getResultType() == IDamageResult.ResultType.EVADED) {
            sb.append(ChatColor.GRAY).append("회피!");
            if (result.getRawDamage() > 0) {
                sb.append(" (").append(String.format("%.1f", result.getRawDamage())).append(" 회피)");
            }
            return sb.toString();
        }

        // 완전 블록
        if (result.getResultType() == IDamageResult.ResultType.FULL_BLOCKED) {
            sb.append(ChatColor.AQUA).append("완벽한 블록!");
            return sb.toString();
        }

        // 크리티컬 (블록 여부와 관계없이 표시)
        if (result.isCritical()) {
            sb.append(ChatColor.GOLD).append("★치명타! ");
            color = ChatColor.GOLD;
        }

        // 블록
        if (result.isBlocked()) {
            sb.append(ChatColor.AQUA).append("블록! ");
        }

        // 데미지 수치
        sb.append(color).append(String.format("%.1f", result.getFinalDamage()));

        // 상세 정보
        sb.append(ChatColor.GRAY).append(" (");
        sb.append("기본: ").append(String.format("%.1f", result.getRawDamage()));

        if (result.getMitigationDamage() > 0) {
            sb.append(", 방어: -").append(String.format("%.1f", result.getMitigationDamage()));
        }

        if (result.getBlockDamage() > 0) {
            sb.append(", 블록: -").append(String.format("%.1f", result.getBlockDamage()));
        }

        sb.append(")");

        return sb.toString();
    }

    private Entity getActualDamager(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Entity shooter) {
                return shooter;
            }
        }

        return damager;
    }
}
