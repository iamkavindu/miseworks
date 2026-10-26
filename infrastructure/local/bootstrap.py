#!/usr/bin/env python3
"""Create only local Floci Cognito resources; never reads AWS credentials."""
import json
import os
from pathlib import Path
import secrets
import time
import urllib.error
import urllib.request

ENDPOINT = 'http://localhost:4566'
POOL = 'us-east-1_miseworks'
CLIENT = 'miseworks-staff-bff'
ENV_FILE = Path(__file__).with_name('.env')


def api(action, payload):
    request = urllib.request.Request(
        ENDPOINT + '/', data=json.dumps(payload).encode(),
        headers={'Content-Type': 'application/x-amz-json-1.1',
                 'X-Amz-Target': 'AWSCognitoIdentityProviderService.' + action})
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        body = json.load(error)
        raise RuntimeError(body.get('__type', '') + ': ' + body.get('message', str(body))) from error


def ensure(action, payload, create_action, create_payload):
    try:
        return api(action, payload)
    except RuntimeError as error:
        if 'ResourceNotFoundException' not in str(error) and 'UserNotFoundException' not in str(error):
            raise
        return api(create_action, create_payload)


def main():
    for attempt in range(60):
        try:
            api('ListUserPools', {'MaxResults': 1})
            break
        except (OSError, RuntimeError):
            if attempt == 59:
                raise
            time.sleep(1)
    existing = {}
    if ENV_FILE.exists():
        existing = dict(line.split('=', 1) for line in ENV_FILE.read_text().splitlines() if '=' in line)
    client_secret = existing.get('COGNITO_CLIENT_SECRET', secrets.token_urlsafe(32))
    password = existing.get('LOCAL_MANAGER_PASSWORD', 'Local-Aa1!' + secrets.token_urlsafe(24))
    ensure('DescribeUserPool', {'UserPoolId': POOL}, 'CreateUserPool', {
        'PoolName': 'miseworks-local',
        'AdminCreateUserConfig': {'AllowAdminCreateUserOnly': True},
        'UserPoolTags': {'floci:override-id': POOL,
                         'floci:override-cognito-client-id': 'use-name',
                         'floci:override-cognito-client-secret': client_secret}})
    client = ensure('DescribeUserPoolClient', {'UserPoolId': POOL, 'ClientId': CLIENT},
                   'CreateUserPoolClient', {
                       'UserPoolId': POOL, 'ClientName': CLIENT, 'GenerateSecret': True,
                       'AllowedOAuthFlowsUserPoolClient': True, 'AllowedOAuthFlows': ['code'],
                       'AllowedOAuthScopes': ['openid', 'profile', 'email'],
                       'SupportedIdentityProviders': ['COGNITO'],
                       'CallbackURLs': ['http://localhost:8081/login/oauth2/code/cognito'],
                       'LogoutURLs': ['http://localhost:8081/logged-out'],
                       'AccessTokenValidity': 15, 'IdTokenValidity': 15, 'RefreshTokenValidity': 1,
                       'TokenValidityUnits': {'AccessToken': 'minutes', 'IdToken': 'minutes', 'RefreshToken': 'days'}})['UserPoolClient']
    ensure('AdminGetUser', {'UserPoolId': POOL, 'Username': 'local-manager'},
           'AdminCreateUser', {'UserPoolId': POOL, 'Username': 'local-manager', 'MessageAction': 'SUPPRESS',
                               'UserAttributes': [{'Name': 'email', 'Value': 'manager@example.invalid'},
                                                  {'Name': 'email_verified', 'Value': 'true'}]})
    api('AdminSetUserPassword', {'UserPoolId': POOL, 'Username': 'local-manager', 'Password': password, 'Permanent': True})
    values = {'COGNITO_ISSUER_URI': ENDPOINT + '/' + POOL, 'COGNITO_CLIENT_ID': client['ClientId'],
              'COGNITO_CLIENT_SECRET': client['ClientSecret'],
              'COGNITO_LOGOUT_URI': ENDPOINT + '/cognito-idp/logout',
              'BFF_LOGOUT_REDIRECT_URI': 'http://localhost:8081/logged-out',
              'LOCAL_MANAGER_USERNAME': 'local-manager', 'LOCAL_MANAGER_PASSWORD': password}
    fd = os.open(ENV_FILE, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    os.fchmod(fd, 0o600)
    with os.fdopen(fd, 'w') as output:
        output.write(''.join(key + '=' + value + '\n' for key, value in values.items()))
    print('Local Cognito ready. Login credentials and BFF configuration saved to infrastructure/local/.env (mode 600).')


if __name__ == '__main__':
    main()
