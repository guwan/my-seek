package top.knos.mySeek.security.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Entity(name = "sso_data_org")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class DataOrg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private Boolean status;
    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;

}
