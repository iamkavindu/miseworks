#!/usr/bin/env python3
"""Verify hosted login and logout against the running local BFF; optional replica check."""
import argparse
from html.parser import HTMLParser
import http.cookiejar
import json
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request


class Form(HTMLParser):
    def __init__(self):
        super().__init__()
        self.fields = {}
        self.action = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'form':
            self.action = attrs.get('action')
        if tag == 'input' and 'name' in attrs:
            self.fields[attrs['name']] = attrs.get('value', '')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--replica-url', help='Optional second BFF, e.g. http://localhost:8082')
    args = parser.parse_args()
    if args.replica_url and urllib.parse.urlparse(args.replica_url).hostname != 'localhost':
        parser.error('Replica must use localhost; this script handles local credentials only.')
    values = dict(line.split('=', 1) for line in Path(__file__).with_name('.env').read_text().splitlines())
    jar = http.cookiejar.CookieJar()
    browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    base = 'http://localhost:8081'
    with browser.open(base + '/oauth2/authorization/cognito', timeout=10) as response:
        page = response.read().decode()
        login_url = response.url
    form = Form()
    form.feed(page)
    assert '_csrf' in form.fields, 'Hosted login form missing'
    form.fields.update(username=values['LOCAL_MANAGER_USERNAME'], password=values['LOCAL_MANAGER_PASSWORD'])
    target = urllib.parse.urljoin(login_url, form.action or login_url)
    with browser.open(target, data=urllib.parse.urlencode(form.fields).encode(), timeout=10) as response:
        identity = json.load(response)
    assert 'subject' in identity and 'token' not in identity, 'Login did not produce safe session information'
    if args.replica_url:
        with browser.open(args.replica_url + '/api/session', timeout=10) as response:
            assert json.load(response) == identity, 'Replica did not load shared session'
    with browser.open(base + '/api/csrf', timeout=10) as response:
        csrf = json.load(response)
    logout = urllib.request.Request(base + '/logout', data=b'', headers={csrf['headerName']: csrf['token']})
    with browser.open(logout, timeout=10) as response:
        assert response.url == base + '/logged-out', 'Provider logout did not return to BFF'
    for endpoint in [base] + ([args.replica_url] if args.replica_url else []):
        try:
            browser.open(endpoint + '/api/session', timeout=10)
        except urllib.error.HTTPError as error:
            assert error.code == 401, 'Unexpected status after logout'
        else:
            raise AssertionError('Session remained authenticated after logout')
    assert not any(cookie.name == 'cognito' and cookie.value for cookie in jar), 'Provider session remained after logout'
    print('PASS: hosted login, OIDC callback, session, CSRF logout and provider sign-out' +
          ('; shared session and logout across two BFF processes.' if args.replica_url else '.'))


if __name__ == '__main__':
    main()
