package id.ac.itb.lumen.vperc;

import id.ac.itb.lumen.core.Rotation3;
import id.ac.itb.lumen.core.Vector3;

/**
 * Created by Sigit on 03/05/2015.
 */
public class Human {

    private String humanId;
    private Vector3 position;
    private Rotation3 rotation;

    public String getHumanId() {
        return humanId;
    }

    public void setHumanId(String humanId) {
        this.humanId = humanId;
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public Rotation3 getRotation() {
        return rotation;
    }

    public void setRotation(Rotation3 rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "Human{" +
                "humanId='" + humanId + '\'' +
                ", position=" + position +
                ", rotation=" + rotation +
                '}';
    }

}
