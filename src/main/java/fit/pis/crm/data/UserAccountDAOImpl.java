package fit.pis.crm.data;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fit.pis.crm.model.UserAccount;

@Repository
@Transactional
@Stateful(name = "UserAccountDAO")
public class UserAccountDAOImpl implements UserAccountDAO {
	
	@PersistenceContext(name = "crm-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager em;

	public UserAccount findById(Long id) {
		return em.find(UserAccount.class, id);
	}

	public UserAccount findByEmail(String email) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<UserAccount> criteria = cb.createQuery(UserAccount.class);
		Root<UserAccount> userAccount = criteria.from(UserAccount.class);
		criteria.select(userAccount).where(cb.equal(userAccount.get("email"), email));
		return em.createQuery(criteria).getSingleResult();
	}

	public List<UserAccount> findAllOrderedByUserName() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<UserAccount> criteria = cb.createQuery(UserAccount.class);
		Root<UserAccount> userAccount = criteria.from(UserAccount.class);

		criteria.select(userAccount).orderBy(cb.asc(userAccount.get("name")));
		return em.createQuery(criteria).getResultList();
	}

	public void register(UserAccount userAccount) {
		em.persist(userAccount);
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			String hashSource = userAccount.getEmail() + userAccount.getPassword() + userAccount.getPhoneNumber()
					+ userAccount.getUserName() + new java.util.Date();
			md.update(hashSource.getBytes("UTF-8"));
			em.merge(userAccount);
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return;
		
	}

	public void update(UserAccount userAccount) {
		em.merge(userAccount);
		
	}
	
	

}