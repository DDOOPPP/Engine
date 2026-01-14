package org.gi.skill;

public enum CCType {
    STUN("기절", true, true),
    SLOW("둔화", false, false),
    ROOT("속박",true,false),
    SILENCE("침묵",false,true),
    KNOCKBACK("넉백",false,false),
    AIRBORN("에어본",true,true),
    FEAR("공포",true,true),
    TAUNT("도발",false,false);

    private final String displayName;
    private final boolean preventsMovement;
    private final boolean preventsAction;

    CCType(String displayName, boolean preventsMovement, boolean preventsAction) {
        this.displayName = displayName;
        this.preventsMovement = preventsMovement;
        this.preventsAction = preventsAction;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPreventsAction() {
        return preventsAction;
    }

    public boolean isPreventsMovement() {
        return preventsMovement;
    }
}