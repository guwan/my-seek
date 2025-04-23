package top.knos.mySeek.security.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.springframework.security.core.GrantedAuthority;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity(name = "sso_authority")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Authority implements GrantedAuthority {

	/**
	 *
	 */
	private static final long serialVersionUID = 6885771464195192436L;
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	private String authority;
	private String name;
	private String mark;
	private Integer sort;

	@ManyToMany
	@JoinTable(name = "sso_authority_resources",
			joinColumns = @JoinColumn(name = "authority_id"),
			inverseJoinColumns = @JoinColumn(name = "resources_id"))
	@ToString.Exclude
	private Set<Resource> resources = new LinkedHashSet<>();


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Authority authority = (Authority) o;
		return id != null && Objects.equals(id, authority.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
