import { Authenticator } from '@aws-amplify/ui-react';
import { Navigate } from 'react-router-dom';

export default function LoginPage() {
  return (
    <Authenticator loginMechanisms={['email']}>
      {() => <Navigate to="/" replace />}
    </Authenticator>
  );
}