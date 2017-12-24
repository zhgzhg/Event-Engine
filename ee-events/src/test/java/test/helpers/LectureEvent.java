package test.helpers;

import net.uniplovdiv.fmi.cs.vrs.event.DomainEvent;
import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;

public class LectureEvent extends DomainEvent {
    private static final long serialVersionUID = 9189168428423476110L;

    @EmbeddedParameter("lecturer")
    private String lecturer;

    @EmbeddedParameter("lecture_subject")
    private String subject;

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
