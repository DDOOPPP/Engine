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

        // PVP
        if (!(targetEntity instanceof Player player)){
            return;
        }

        PlayerStatHolder target = statManager.getOrLoad(player);
        PlayerStatHolder attacker = null;

        if (attackerEntity instanceof Player attackerPlayer){
            attacker = statManager.getOrLoad(attackerPlayer);
        }

        DamageType damageType = mapDamageType(event.getCause());
        double baseDamage = event.getDamage();

        IDamageSource source = DamageSourceBuilder.create()
                .attacker(attacker)
                .damageType(damageType)
                .baseDamage(baseDamage)
                .skillScaling(attacker != null ? 1.0 : 0)
                .sourceId(getSourceID(event))
                .displayName(getDisplayName(event))
                .canCritical(attacker != null)
                .canBlock(true)
                .canEvade(true)
                .build();

        IDamageResult result = damageCalculator.calculate(source,target);

        event.setDamage(result.getFinalDamage());

        sendDamageMessage(player, attackerEntity, result);//추후 토글화 예정
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

    private void sendDamageMessage(Player victim, Entity damager, IDamageResult result) {
        // 피해자에게 메시지
        String message = buildDamageMessage(result, false);
        victim.sendMessage(message);

        // 공격자에게 메시지
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

        // 결과 타입에 따른 색상
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

        // 결과 타입
        switch (result.getResultType()) {
            case EVADED -> {
                sb.append(ChatColor.GRAY).append("회피!");
                return sb.toString();
            }
            case CRITICAL -> sb.append(ChatColor.GOLD).append("★치명타! ");
            case BLOCKED -> sb.append(ChatColor.AQUA).append("블록! ");
            case FULL_BLOCKED -> {
                sb.append(ChatColor.AQUA).append("완벽한 블록!");
                return sb.toString();
            }
            default -> {}
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
